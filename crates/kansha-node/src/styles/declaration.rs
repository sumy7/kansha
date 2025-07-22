use std::ops::Range;
use crate::styles::CustomParseError;

use cssparser::*;

#[derive(Debug, PartialEq, Clone)]
#[allow(unused_lifetimes)]
pub struct DeclarationBlock {
    pub declarations: Vec<Declaration>,
}

/// CSS declaration
#[derive(Debug, Clone, PartialEq)]
pub struct Declaration {
    pub property: String,
    pub value: String,
    pub important: bool,
}

impl<'i> DeclarationBlock {
    pub fn parse<'a, 't>(
        input: &mut Parser<'i, 't>,
    ) -> Result<Self, ParseError<'i, CustomParseError<'i>>> {
        let mut declarations = DeclarationList::new();
        let mut decl_parser = PropertyDeclarationParser {
            declarations: &mut declarations,
        };
        let parser = RuleBodyParser::new(input, &mut decl_parser);
        for res in parser {
            if let Err((err, _)) = res {
                continue;
            }
        }

        Ok(DeclarationBlock { declarations })
    }
}

struct PropertyDeclarationParser<'a> {
    declarations: &'a mut Vec<Declaration>,
}

impl<'i> DeclarationParser<'i> for PropertyDeclarationParser<'_> {
    type Declaration = ();
    type Error = CustomParseError<'i>;

    fn parse_value<'t>(
        &mut self,
        name: CowRcStr<'i>,
        input: &mut Parser<'i, 't>,
        _state: &ParserState,
    ) -> Result<Self::Declaration, ParseError<'i, Self::Error>> {
        parse_declaration(name, input, self.declarations)
    }
}

pub(crate) fn parse_declaration<'i>(
    name: CowRcStr<'i>,
    input: &mut Parser<'i, '_>,
    declarations: &mut DeclarationList,
) -> Result<(), ParseError<'i, CustomParseError<'i>>> {
    let start = input.position();
    let _: Result<_, ParseError<'_, ()>> = input.parse_until_before(Delimiter::Bang, |_| Ok(()));
    let end = input.position();
    let value = input.slice(Range{start, end}).trim().to_string();

    let important = input
        .try_parse(|input| {
            input.expect_delim('!')?;
            input.expect_ident_matching("important")
        })
        .is_ok();

    declarations.push(Declaration {
        property: name.to_string(),
        value,
        important,
    });
    Ok(())
}

impl<'i> AtRuleParser<'i> for PropertyDeclarationParser<'_> {
    type Prelude = ();
    type AtRule = ();
    type Error = CustomParseError<'i>;
}

impl<'i> QualifiedRuleParser<'i> for PropertyDeclarationParser<'_> {
    type Prelude = ();
    type QualifiedRule = ();
    type Error = CustomParseError<'i>;
}

impl<'i> RuleBodyItemParser<'i, (), CustomParseError<'i>> for PropertyDeclarationParser<'_> {
    fn parse_qualified(&self) -> bool {
        false
    }

    fn parse_declarations(&self) -> bool {
        true
    }
}

pub(crate) type DeclarationList = Vec<Declaration>;
