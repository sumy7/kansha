use crate::styles::{ComputedStyle, DeclarationBlock};
use cssparser::{Parser, ParserInput};
use std::fmt::Debug;

pub fn parse_style_attribute(
    input: &str,
) {
    let mut parser_input = ParserInput::new(input);
    let mut parser = Parser::new(&mut parser_input);

    let declaration_block = DeclarationBlock::parse(&mut parser);

    let mut computed_style = ComputedStyle::default();

    match declaration_block {
        Ok(block) => {
            for declaration in block.declarations {
                computed_style.apply_declaration(&declaration);
                println!("{}: {}", declaration.property, declaration.value);
            }
        }
        Err(err) => {
            eprintln!("Error parsing style attribute: {:?}", err);
        }
    }

    println!("Computed Style: {:?}", computed_style);
}

#[test]
fn test_parse_style_attribute() {
    let input = "color: red; font-size: 16px;";
    parse_style_attribute(input);
    // You can add assertions here to check the parsed output
}