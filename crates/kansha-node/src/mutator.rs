use crate::document::{Document, DocumentMutator};
use crate::node::attributes::Attribute;
use crate::{qual_name, NodeId};
use dioxus_core::{
    AttributeValue, ElementId, Template, TemplateAttribute, TemplateNode, WriteMutations,
};
use rustc_hash::FxHashMap;

/// The state of the Dioxus integration with the RealDom
#[derive(Debug)]
pub struct DioxusState {
    /// Store of templates keyed by unique name
    templates: FxHashMap<Template, Vec<NodeId>>,
    /// Stack machine state for applying dioxus mutations
    stack: Vec<NodeId>,
    /// Mapping from vdom ElementId -> rdom NodeId
    node_id_mapping: Vec<Option<NodeId>>,
}

impl DioxusState {
    /// Initialize the DioxusState in the RealDom
    pub fn create(root_id: usize) -> Self {
        Self {
            templates: FxHashMap::default(),
            stack: vec![root_id],
            node_id_mapping: vec![Some(root_id)],
        }
    }

    /// Convert an ElementId to a NodeId
    pub fn element_to_node_id(&self, element_id: ElementId) -> NodeId {
        self.try_element_to_node_id(element_id).unwrap()
    }

    /// Attempt to convert an ElementId to a NodeId. This will return None if the ElementId is not in the RealDom.
    pub fn try_element_to_node_id(&self, element_id: ElementId) -> Option<NodeId> {
        self.node_id_mapping.get(element_id.0).copied().flatten()
    }

    pub(crate) fn anchor_and_nodes(&mut self, id: ElementId, m: usize) -> (usize, Vec<usize>) {
        let anchor_node_id = self.element_to_node_id(id);
        let new_nodes = self.m_stack_nodes(m);
        (anchor_node_id, new_nodes)
    }

    pub(crate) fn m_stack_nodes(&mut self, m: usize) -> Vec<usize> {
        self.stack.split_off(self.stack.len() - m)
    }
}

/// A writer for mutations that can be used with the RealDom.
pub struct MutationWriter<'a> {
    /// The realdom associated with this writer
    pub docm: DocumentMutator<'a>,
    /// The state associated with this writer
    pub state: &'a mut DioxusState,
}

impl<'a> MutationWriter<'a> {
    pub fn new(doc: &'a mut Document, state: &'a mut DioxusState) -> Self {
        MutationWriter {
            docm: doc.mutate(),
            state,
        }
    }

    /// 更新 ElementId -> NodeId 的映射
    fn set_id_mapping(&mut self, element_id: ElementId, node_id: NodeId) {
        let element_id: usize = element_id.0;

        // 确保 node_id_mapping 的长度足够大
        if self.state.node_id_mapping.len() <= element_id {
            self.state.node_id_mapping.resize(element_id + 1, None);
        }

        // 更新映射
        self.state.node_id_mapping[element_id] = Some(node_id);
    }

    /// 创建一个 ElementId -> NodeId 的映射，并将 node 压入 stack 中
    fn map_new_node(&mut self, node_id: NodeId, element_id: ElementId) {
        self.set_id_mapping(element_id, node_id);
        self.state.stack.push(node_id);
    }

    fn load_child(&self, path: &[u8]) -> NodeId {
        let top_of_stack_node_id = *self.state.stack.last().unwrap();
        self.docm.node_at_path(top_of_stack_node_id, path)
    }
}

impl WriteMutations for MutationWriter<'_> {
    fn append_children(&mut self, id: ElementId, m: usize) {
        let (parent_id, child_node_ids) = self.state.anchor_and_nodes(id, m);
        self.docm.append_children(parent_id, &child_node_ids);
    }

    fn assign_node_id(&mut self, path: &'static [u8], id: ElementId) {
        if let Some(node_id) = self.state.try_element_to_node_id(id) {
            self.docm.remove_node_if_unparented(node_id);
        }

        self.set_id_mapping(id, self.load_child(path));
    }

    fn create_placeholder(&mut self, id: ElementId) {
        let node_id = self.docm.create_comment_node();
        self.map_new_node(node_id, id);
    }

    fn create_text_node(&mut self, value: &str, id: ElementId) {
        let node_id = self.docm.create_text_node(value);
        self.map_new_node(node_id, id);
    }

    fn load_template(&mut self, template: Template, index: usize, id: ElementId) {
        let template_entry = self.state.templates.entry(template).or_insert_with(|| {
            let template_root_ids: Vec<NodeId> = template
                .roots
                .iter()
                .map(|root| create_template_node(&mut self.docm, root))
                .collect();

            template_root_ids
        });

        let template_node_id = template_entry[index];
        let clone_id = self.docm.deep_clone_node(template_node_id);

        self.map_new_node(clone_id, id);
    }

    fn replace_node_with(&mut self, id: ElementId, m: usize) {
        let (anchor_node_id, new_node_ids) = self.state.anchor_and_nodes(id, m);
        self.docm.replace_node_with(anchor_node_id, &new_node_ids);
    }

    fn replace_placeholder_with_nodes(&mut self, path: &'static [u8], m: usize) {
        let new_node_ids = self.state.m_stack_nodes(m);
        let anchor_node_id = self.load_child(path);
        self.docm.replace_node_with(anchor_node_id, &new_node_ids);
    }

    fn insert_nodes_after(&mut self, id: ElementId, m: usize) {
        let (anchor_node_id, new_node_ids) = self.state.anchor_and_nodes(id, m);
        self.docm.insert_nodes_after(anchor_node_id, &new_node_ids);
    }

    fn insert_nodes_before(&mut self, id: ElementId, m: usize) {
        let (anchor_node_id, new_node_ids) = self.state.anchor_and_nodes(id, m);
        self.docm.insert_nodes_before(anchor_node_id, &new_node_ids);
    }

    fn set_attribute(
        &mut self,
        local_name: &'static str,
        ns: Option<&'static str>,
        value: &AttributeValue,
        id: ElementId,
    ) {
        let node_id = self.state.element_to_node_id(id);

        if ns == Some("style") {
            return;
        }

        fn is_falsy(val: &AttributeValue) -> bool {
            match val {
                AttributeValue::None => true,
                AttributeValue::Text(val) => val == "false",
                AttributeValue::Bool(val) => !val,
                AttributeValue::Int(val) => *val == 0,
                AttributeValue::Float(val) => *val == 0.0,
                _ => false,
            }
        }

        let name = qual_name(local_name, ns);

        if value == &AttributeValue::None || (local_name == "checked" && is_falsy(value)) {
            self.docm.clear_attribute(node_id, name);
        } else {
            match value {
                AttributeValue::Text(value) => self.docm.set_attribute(node_id, name, value),
                AttributeValue::Float(value) => {
                    let value = value.to_string();
                    self.docm.set_attribute(node_id, name, &value);
                }
                AttributeValue::Int(value) => {
                    let value = value.to_string();
                    self.docm.set_attribute(node_id, name, &value);
                }
                AttributeValue::Bool(value) => {
                    let value = value.to_string();
                    self.docm.set_attribute(node_id, name, &value);
                }
                _ => {
                    // FIXME: support all attribute types
                }
            };
        }
    }

    fn set_node_text(&mut self, value: &str, id: ElementId) {
        let node_id = self.state.element_to_node_id(id);
        self.docm.set_node_text(node_id, value);
    }

    fn create_event_listener(&mut self, name: &'static str, id: ElementId) {
        // 在 kansha 环境中不处理 event 事件
        // do nothing
    }

    fn remove_event_listener(&mut self, name: &'static str, id: ElementId) {
        // 在 kansha 环境中不处理 event 事件
        // do nothing
    }

    fn remove_node(&mut self, id: ElementId) {
        let node_id = self.state.element_to_node_id(id);
        self.docm.remove_node(node_id);
    }

    fn push_root(&mut self, id: ElementId) {
        let node_id = self.state.element_to_node_id(id);
        self.state.stack.push(node_id);
    }
}

fn create_template_node(docm: &mut DocumentMutator<'_>, node: &TemplateNode) -> NodeId {
    match node {
        TemplateNode::Element {
            tag,
            namespace,
            attrs,
            children,
        } => {
            let name = qual_name(tag, *namespace);
            let attrs = attrs.iter().filter_map(map_template_attr).collect();
            let node_id = docm.create_element(name, attrs);

            let child_ids: Vec<NodeId> = children
                .iter()
                .map(|child| create_template_node(docm, child))
                .collect();

            docm.append_children(node_id, &child_ids);

            node_id
        }
        TemplateNode::Text { text } => docm.create_text_node(text),
        TemplateNode::Dynamic { .. } => docm.create_comment_node(),
    }
}

fn map_template_attr(attr: &TemplateAttribute) -> Option<Attribute> {
    let TemplateAttribute::Static {
        name,
        value,
        namespace,
    } = attr
    else {
        return None;
    };

    let name = qual_name(name, *namespace);
    let value = value.to_string();
    Some(Attribute { name, value })
}
