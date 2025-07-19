use crate::tree::layout::Layout;
use kansha_interface::node::NodeId;
use std::fmt::Debug;

// pub struct LayoutTreeNode {
//     pub id: NodeId,
//     pub properties: C::CssPropertyMap,
//     pub children: Vec<NodeId>,
//     pub parent: Option<NodeId>,
//     pub name: String,
//     pub namespace: Option<String>,
//     pub data: RenderNodeData<C>,
//     pub layout: Layout,
// }
//
// impl Debug for LayoutTreeNode {
//     fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
//         f.debug_struct("RenderTreeNode")
//             .field("id", &self.id)
//             .field("properties", &self.properties)
//             .field("children", &self.children)
//             .field("parent", &self.parent)
//             .field("name", &self.name)
//             .field("namespace", &self.namespace)
//             .field("data", &self.data)
//             .finish()
//     }
// }
