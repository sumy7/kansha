use crate::node::attributes::{Attribute, Attributes};
use crate::styles::{ComputedStyle, Declaration, parse_style_attribute};
use markup5ever::{LocalName, QualName, local_name};
use slab::Slab;
use std::cell::{Cell, RefCell};
use std::fmt::Write;
use taffy::Layout;

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum DisplayOuter {
    Block,
    Inline,
    None,
}

pub struct Node {
    // The actual tree we belong to. This is unsafe!!
    tree: *mut Slab<Node>,

    /// Our Id
    pub id: usize,
    /// Our parent's ID
    pub parent: Option<usize>,
    // What are our children?
    pub children: Vec<usize>,
    /// Our parent in the layout hierachy: a separate list that includes anonymous collections of inline elements
    pub layout_parent: Cell<Option<usize>>,
    /// A separate child list that includes anonymous collections of inline elements
    pub layout_children: RefCell<Option<Vec<usize>>>,
    /// The same as layout_children, but sorted by z-index
    pub paint_children: RefCell<Option<Vec<usize>>>,

    /// Node type (Element, TextNode, etc) specific data
    pub data: NodeData,

    // Taffy layout data:
    pub style: ComputedStyle,
    // pub has_snapshot: bool,
    // pub snapshot_handled: AtomicBool,
    pub display_outer: DisplayOuter,
    // pub cache: Cache,
    // pub unrounded_layout: Layout,
    pub final_layout: Layout,
}

impl Node {
    pub(crate) fn new(tree: *mut Slab<Node>, id: usize, data: NodeData) -> Self {
        Self {
            tree,
            id,
            parent: None,
            children: vec![],
            layout_parent: Cell::new(None),
            layout_children: RefCell::new(None),
            paint_children: RefCell::new(None),
            data,
            style: ComputedStyle::default(),
            display_outer: DisplayOuter::Block,
            final_layout: Layout::new(),
        }
    }

    pub fn tree(&self) -> &Slab<Node> {
        unsafe { &*self.tree }
    }

    #[track_caller]
    pub fn with(&self, id: usize) -> &Node {
        self.tree().get(id).unwrap()
    }

    pub fn print_tree(&self, level: usize) {
        println!(
            "{} {} {:?} {} {:?}",
            "  ".repeat(level),
            self.id,
            self.parent,
            self.node_debug_str().replace('\n', ""),
            self.children
        );
        // println!("{} {:?}", "  ".repeat(level), self.children);
        for child_id in self.children.iter() {
            let child = self.with(*child_id);
            child.print_tree(level + 1)
        }
    }

    // Get the index of the current node in the parents child list
    pub fn index_of_child(&self, child_id: usize) -> Option<usize> {
        self.children.iter().position(|id| *id == child_id)
    }

    // Get the index of the current node in the parents child list
    pub fn child_index(&self) -> Option<usize> {
        self.tree()[self.parent?]
            .children
            .iter()
            .position(|id| *id == self.id)
    }

    // Get the nth node in the parents child list
    pub fn forward(&self, n: usize) -> Option<&Node> {
        let child_idx = self.child_index().unwrap_or(0);
        self.tree()[self.parent?]
            .children
            .get(child_idx + n)
            .map(|id| self.with(*id))
    }

    pub fn backward(&self, n: usize) -> Option<&Node> {
        let child_idx = self.child_index().unwrap_or(0);
        if child_idx < n {
            return None;
        }

        self.tree()[self.parent?]
            .children
            .get(child_idx - n)
            .map(|id| self.with(*id))
    }

    pub fn element_data(&self) -> Option<&ElementData> {
        match self.data {
            NodeData::Element(ref data) => Some(data),
            _ => None,
        }
    }

    pub fn element_data_mut(&mut self) -> Option<&mut ElementData> {
        match self.data {
            NodeData::Element(ref mut data) => Some(data),
            _ => None,
        }
    }

    pub fn text_data(&self) -> Option<&TextNodeData> {
        match self.data {
            NodeData::Text(ref data) => Some(data),
            _ => None,
        }
    }

    pub fn text_data_mut(&mut self) -> Option<&mut TextNodeData> {
        match self.data {
            NodeData::Text(ref mut data) => Some(data),
            _ => None,
        }
    }

    pub fn attrs(&self) -> Option<&[Attribute]> {
        Some(&self.element_data()?.attrs)
    }

    pub fn attr(&self, name: LocalName) -> Option<&str> {
        let attr = self.attrs()?.iter().find(|id| id.name.local == name)?;
        Some(&attr.value)
    }

    pub fn node_debug_str(&self) -> String {
        let mut s = String::new();

        match &self.data {
            NodeData::Text(data) => {
                let bytes = data.content.as_bytes();
                write!(
                    s,
                    "TEXT {}",
                    &std::str::from_utf8(bytes.split_at(10.min(bytes.len())).0)
                        .unwrap_or("INVALID UTF8")
                )
            }
            NodeData::Comment => write!(
                s,
                "COMMENT",
                // &std::str::from_utf8(data.contents.as_bytes().split_at(10).0).unwrap_or("INVALID UTF8")
            ),
            NodeData::Element(data) => {
                let name = &data.name;
                let class = self.attr(local_name!("class")).unwrap_or("");
                if !class.is_empty() {
                    write!(
                        s,
                        "<{} class=\"{}\"> ({:?})",
                        name.local, class, self.display_outer
                    )
                } else {
                    write!(s, "<{}> ({:?})", name.local, self.display_outer)
                }
            }
        }
        .unwrap();
        s
    }
}

#[derive(Debug, Clone)]
pub enum NodeData {
    /// A DOM element with attributes.
    Element(ElementData),
    /// A text node.
    Text(TextNodeData),
    /// A comment.
    Comment,
}

#[derive(Debug, Clone)]
pub struct ElementData {
    /// The elements tag name, namespace and prefix
    pub name: QualName,

    /// The elements id attribute parsed as an atom (if it has one)
    pub id: Option<String>,

    /// The element's attributes
    pub attrs: Attributes,

    /// The element's parsed style attribute
    pub style_attribute: Option<Vec<Declaration>>,
}

impl ElementData {
    pub fn new(name: QualName, attrs: Vec<Attribute>) -> Self {
        let id_attr_atom = attrs
            .iter()
            .find(|attr| &attr.name.local == "id")
            .map(|attr| attr.value.as_ref())
            .map(|value: &str| value.to_string());

        let mut data = ElementData {
            name,
            id: id_attr_atom,
            attrs: Attributes::new(attrs),
            style_attribute: Default::default(),
        };

        data
    }

    pub fn attrs(&self) -> &[Attribute] {
        &self.attrs
    }

    pub fn attr(&self, name: impl PartialEq<LocalName>) -> Option<&str> {
        let attr = self.attrs.iter().find(|attr| name == attr.name.local)?;
        Some(&attr.value)
    }

    pub fn flush_style_attribute(&mut self) {
        self.style_attribute =
            parse_style_attribute(self.attr(local_name!("style")).unwrap_or_default());
    }
}

#[derive(Debug, Clone)]
pub struct TextNodeData {
    /// The textual content of the text node
    pub content: String,
}

impl TextNodeData {
    pub fn new(content: String) -> Self {
        Self { content }
    }
}
