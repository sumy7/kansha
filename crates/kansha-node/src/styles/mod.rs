use cssparser::{Parser, ParserInput};

mod declaration;
pub use declaration::*;

pub mod error;
pub use error::*;

pub mod property;
pub use property::*;

pub mod taffy_style;
pub use taffy_style::*;

/// Parses a style attribute string and applies the styles to the provided ComputedStyle
pub fn parse_style_attribute_to(input: &str, style: &mut ComputedStyle) {
    let mut parser_input = ParserInput::new(input);
    let mut parser = Parser::new(&mut parser_input);
    let declaration_block = DeclarationBlock::parse(&mut parser);
    match declaration_block {
        Ok(block) => {
            for declaration in block.declarations {
                style.apply_declaration(&declaration);
            }
        }
        Err(err) => {
            eprintln!("Error parsing style attribute: {:?}", err);
        }
    }
}

/// Parses a style attribute string and returns a vector of declarations
pub fn parse_style_attribute(input: &str) -> Option<Vec<Declaration>> {
    let mut parser_input = ParserInput::new(input);
    let mut parser = Parser::new(&mut parser_input);
    let declaration_block = DeclarationBlock::parse(&mut parser);
    match declaration_block {
        Ok(block) => Some(block.declarations),
        Err(err) => {
            eprintln!("Error parsing style attribute: {:?}", err);
            None
        }
    }
}
