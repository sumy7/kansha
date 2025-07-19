use markup5ever::{ns, LocalName, Namespace, QualName};

pub mod document;
pub mod mutator;
pub mod node;

pub type NodeId = usize;

pub(crate) fn qual_name(local_name: &str, namespace: Option<&str>) -> QualName {
    QualName {
        prefix: None,
        ns: namespace.map(Namespace::from).unwrap_or(ns!(html)),
        local: LocalName::from(local_name),
    }
}
