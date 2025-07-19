use crate::node::attributes::Attribute;
use crate::node::node::{ElementData, Node, NodeData, TextNodeData};
use crate::qual_name;
use markup5ever::{local_name, QualName};
use slab::Slab;
use std::collections::{HashMap, HashSet};

pub struct Document {
    pub(crate) nodes: Box<Slab<Node>>,
    pub(crate) nodes_to_id: HashMap<String, usize>,
}

impl Document {
    pub fn new() -> Self {
        let mut doc = Document {
            nodes: Box::new(Slab::new()),
            nodes_to_id: HashMap::new(),
        };

        let mut mutr = doc.mutate();
        mutr.create_element(qual_name("root", None), vec![]);

        doc
    }

    pub fn root_node(&self) -> &Node {
        &self.nodes[0]
    }

    pub fn root_node_mut(&mut self) -> &mut Node {
        &mut self.nodes[0]
    }

    pub fn get_node(&self, node_id: usize) -> Option<&Node> {
        self.nodes.get(node_id)
    }

    pub fn get_node_mut(&mut self, node_id: usize) -> Option<&mut Node> {
        self.nodes.get_mut(node_id)
    }

    pub fn mutate(&mut self) -> DocumentMutator {
        DocumentMutator::new(self)
    }
}

// traversal
impl Document {
    pub fn iter_subtree_mut(&mut self, node_id: usize, mut cb: impl FnMut(usize, &mut Document)) {
        cb(node_id, self);
        iter_subtree_mut_inner(self, node_id, &mut cb);
        fn iter_subtree_mut_inner(
            doc: &mut Document,
            node_id: usize,
            cb: &mut impl FnMut(usize, &mut Document),
        ) {
            let children = std::mem::take(&mut doc.nodes[node_id].children);
            for child_id in children.iter().cloned() {
                cb(child_id, doc);
                iter_subtree_mut_inner(doc, child_id, cb);
            }
            doc.nodes[node_id].children = children;
        }
    }
}

impl Document {
    pub fn create_node(&mut self, node_data: NodeData) -> usize {
        let slab_ptr = self.nodes.as_mut() as *mut Slab<Node>;

        let entry = self.nodes.vacant_entry();
        let id = entry.key();
        entry.insert(Node::new(slab_ptr, id, node_data));
        id
    }

    pub fn create_text_node(&mut self, text: &str) -> usize {
        let content = text.to_string();
        let data = NodeData::Text(TextNodeData::new(content));
        self.create_node(data)
    }

    pub fn deep_clone_node(&mut self, node_id: usize) -> usize {
        let node = &self.nodes[node_id];
        let data = node.data.clone();
        let children = node.children.clone();

        let new_node_id = self.create_node(data);

        let new_children: Vec<usize> = children
            .into_iter()
            .map(|child_id| self.deep_clone_node(child_id))
            .collect();

        for &child_id in &new_children {
            self.nodes[child_id].parent = Some(new_node_id);
        }
        self.nodes[new_node_id].children = new_children;

        new_node_id
    }
}

pub struct DocumentMutator<'doc> {
    pub doc: &'doc mut Document,
}

impl DocumentMutator<'_> {
    pub fn new<'doc>(doc: &'doc mut Document) -> DocumentMutator<'doc> {
        DocumentMutator { doc }
    }

    pub fn node_has_parent(&self, node_id: usize) -> bool {
        self.doc.nodes[node_id].parent.is_some()
    }

    pub fn previous_sibling_id(&self, node_id: usize) -> Option<usize> {
        self.doc.nodes[node_id].backward(1).map(|node| node.id)
    }

    pub fn next_sibling_id(&self, node_id: usize) -> Option<usize> {
        self.doc.nodes[node_id].forward(1).map(|node| node.id)
    }

    pub fn parent_id(&self, node_id: usize) -> Option<usize> {
        self.doc.nodes[node_id].parent
    }

    pub fn node_at_path(&self, start_node_id: usize, path: &[u8]) -> usize {
        let mut current = &self.doc.nodes[start_node_id];
        for i in path {
            let new_id = current.children[*i as usize];
            current = &self.doc.nodes[new_id];
        }
        current.id
    }

    // Node creation methods

    pub fn create_comment_node(&mut self) -> usize {
        self.doc.create_node(NodeData::Comment)
    }

    pub fn create_text_node(&mut self, text: &str) -> usize {
        self.doc.create_text_node(text)
    }

    pub fn create_element(&mut self, name: QualName, attrs: Vec<Attribute>) -> usize {
        let data = ElementData::new(name, attrs);
        let id = self.doc.create_node(NodeData::Element(data));
        id
    }

    pub fn deep_clone_node(&mut self, node_id: usize) -> usize {
        self.doc.deep_clone_node(node_id)
    }

    // Node mutation methods

    pub fn set_node_text(&mut self, node_id: usize, value: &str) {
        let node = self.doc.get_node_mut(node_id).unwrap();

        let text = match node.data {
            NodeData::Text(ref mut text) => text,
            _ => return,
        };

        let changed = text.content != value;
        if !changed {
            return;
        }

        text.content.clear();
        text.content.push_str(value);
    }

    pub fn add_attrs_if_missing(&mut self, node_id: usize, attrs: Vec<Attribute>) {
        let node = &mut self.doc.nodes[node_id];
        let element_data = node.element_data_mut().expect("Not an element");

        let existing_names = element_data
            .attrs
            .iter()
            .map(|e| e.name.clone())
            .collect::<HashSet<_>>();

        for attr in attrs
            .into_iter()
            .filter(|attr| !existing_names.contains(&attr.name))
        {
            self.set_attribute(node_id, attr.name, &attr.value);
        }
    }

    pub fn set_attribute(&mut self, node_id: usize, name: QualName, value: &str) {
        let node = &mut self.doc.nodes[node_id];

        let NodeData::Element(ref mut element) = node.data else {
            return;
        };

        element.attrs.set(name.clone(), value);
    }

    pub fn clear_attribute(&mut self, node_id: usize, name: QualName) {
        let node = &mut self.doc.nodes[node_id];

        let Some(element) = node.element_data_mut() else {
            return;
        };

        let removed_attr = element.attrs.remove(&name);
        let had_attr = removed_attr.is_some();
        if !had_attr {
            return;
        }
    }

    pub fn remove_node(&mut self, node_id: usize) {
        let node = &mut self.doc.nodes[node_id];

        if let Some(parent_id) = node.parent.take() {
            let parent = &mut self.doc.nodes[parent_id];
            parent.children.retain(|id| *id != node_id);
        }

        self.process_removed_subtree(node_id);
    }

    pub fn remove_and_drop_node(&mut self, node_id: usize) -> Option<Node> {
        self.process_removed_subtree(node_id);

        fn remove_node_ignoring_parent(mutr: &mut DocumentMutator, node_id: usize) -> Option<Node> {
            let mut node = mutr.doc.nodes.try_remove(node_id);
            if let Some(node) = &mut node {
                for &child in &node.children {
                    remove_node_ignoring_parent(mutr, child);
                }
            }
            node
        }

        let node = remove_node_ignoring_parent(self, node_id);

        if let Some(parent_id) = node.as_ref().and_then(|node| node.parent) {
            let parent = &mut self.doc.nodes[parent_id];
            parent.children.retain(|id| *id != node_id);
        }

        node
    }

    // Tree mutation methods

    pub fn remove_node_if_unparented(&mut self, node_id: usize) {
        if let Some(node) = self.doc.get_node(node_id) {
            if node.parent.is_none() {
                self.remove_and_drop_node(node_id);
            }
        }
    }

    pub fn append_children(&mut self, parent_id: usize, child_ids: &[usize]) {
        self.add_children_to_parent(parent_id, child_ids, &|parent, child_ids| {
            parent.children.extend_from_slice(&child_ids);
        })
    }

    pub fn insert_nodes_before(&mut self, anchor_node_id: usize, new_node_ids: &[usize]) {
        let parent_id = self.doc.nodes[anchor_node_id].parent.unwrap();
        self.add_children_to_parent(parent_id, new_node_ids, &|parent, child_ids| {
            let node_child_idx = parent.index_of_child(anchor_node_id).unwrap();
            parent
                .children
                .splice(node_child_idx..node_child_idx, child_ids.iter().copied());
        });
    }

    pub fn add_children_to_parent(
        &mut self,
        parent_id: usize,
        child_ids: &[usize],
        insert_children_fn: &dyn Fn(&mut Node, &[usize]),
    ) {
        let new_parent = &mut self.doc.nodes[parent_id];

        insert_children_fn(new_parent, child_ids);

        for child_id in child_ids.iter().copied() {
            let child = &mut self.doc.nodes[child_id];
            let old_parent_id = child.parent.replace(parent_id);

            self.process_added_subtree(child_id);

            if let Some(old_parent_id) = old_parent_id {
                let old_parent = &mut self.doc.nodes[old_parent_id];

                old_parent.children.retain(|id| *id != child_id);
            }
        }
    }

    pub fn insert_nodes_after(&mut self, anchor_node_id: usize, new_node_ids: &[usize]) {
        match self.next_sibling_id(anchor_node_id) {
            Some(id) => self.insert_nodes_before(id, new_node_ids),
            None => {
                let parent_id = self.parent_id(anchor_node_id).unwrap();
                self.append_children(parent_id, new_node_ids)
            }
        }
    }

    pub fn replace_node_with(&mut self, anchor_node_id: usize, new_node_ids: &[usize]) {
        self.insert_nodes_before(anchor_node_id, new_node_ids);
        self.remove_node(anchor_node_id);
    }
}

impl DocumentMutator<'_> {
    fn process_added_subtree(&mut self, node_id: usize) {
        self.doc.iter_subtree_mut(node_id, |node_id, doc| {
            let node = &mut doc.nodes[node_id];

            if let Some(id_attr) = node.attr(local_name!("id")) {
                doc.nodes_to_id.insert(id_attr.to_string(), node_id);
            }
        })
    }

    fn process_removed_subtree(&mut self, node_id: usize) {
        self.doc.iter_subtree_mut(node_id, |node_id, doc| {
            let node = &mut doc.nodes[node_id];

            if let Some(id_attr) = node.attr(local_name!("id")) {
                doc.nodes_to_id.remove(id_attr);
            }
        })
    }
}
