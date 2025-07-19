use dioxus::html::completions::CompleteWithBraces::template;
use dioxus_core::{Element, ScopeId, TemplateNode, VirtualDom};
use kansha_node::document::Document;
use kansha_node::mutator::{DioxusState, MutationWriter};
use kansha_node::node::node::{ElementData, NodeData};

type AppComponent = fn() -> Element;

pub fn kansha(element: AppComponent) {
    println!("Kansha core library initialized!");

    let mut vdom = VirtualDom::new(element);

    let mut document = Document::new();
    let root_element_id = document.root_node().id;
    let mut state = DioxusState::create(root_element_id);
    let mut writer = MutationWriter::new(&mut document, &mut state);

    vdom.mark_dirty(ScopeId::APP);
    vdom.rebuild(&mut writer);

    document.get_node(root_element_id).unwrap().print_tree(0);

    // let commands = vdom.rebuild_to_vec();
    // // vdom.render_suspense_immediate();
    //
    // println!("commands: {:#?}", commands.edits);
    //
    // let node = vdom.get_scope(ScopeId::ROOT).unwrap().root_node();
    // println!("Root node: {:#?}", node);
    //
    // for (root_idx, root) in node.template.roots.iter().enumerate() {
    //     println!("Root {}: {:#?}", root_idx, root);
    //     match root {
    //         TemplateNode::Element {
    //             tag,
    //             attrs,
    //             children,
    //             ..
    //         } => {
    //             println!("Tag: {}", tag);
    //             // for (attr_name, attr_value) in attrs {
    //             //     println!("  Attribute: {} = {}", attr_name, attr_value);
    //             // }
    //             for (child_idx, child) in children.iter().enumerate() {
    //                 println!("  Child {}: {:#?}", child_idx, child);
    //             }
    //         }
    //         TemplateNode::Text {text} => {
    //             println!("Text Node: {}", text);
    //         }
    //         TemplateNode::Dynamic{ id: idx} => {
    //             println!("Dynamic Node: {}", idx);
    //         }
    //     }
    // }
}
