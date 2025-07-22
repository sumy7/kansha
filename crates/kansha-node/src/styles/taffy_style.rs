use crate::styles::Declaration;
use taffy::{AlignItems, Display, FlexDirection, JustifyContent, Style};

/// CSS color value representation
#[derive(Debug, Clone, PartialEq)]
pub enum ColorValue {
    Named(String),
    Hex(String),
    Rgb(u8, u8, u8),
}

/// CSS length value representation
#[derive(Debug, Clone, PartialEq)]
pub enum LengthValue {
    Px(f32),
    Em(f32),
    Percent(f32),
}

/// Simple color representation
#[derive(Debug, Clone, PartialEq)]
pub struct ColorF {
    pub r: f32,
    pub g: f32,
    pub b: f32,
    pub a: f32,
}

impl ColorF {
    pub fn new(r: f32, g: f32, b: f32, a: f32) -> Self {
        Self { r, g, b, a }
    }
}

#[derive(Debug, Clone)]
pub struct ComputedStyle {
    pub color: Option<ColorValue>,
    pub background_color: Option<ColorValue>,
    pub font_size: Option<LengthValue>,
    pub font_weight: Option<String>,
    pub border_width: Option<LengthValue>,
    pub border_color: Option<ColorValue>,
    pub layout_style: Style,
    pub display: DisplayType,
}

#[derive(Debug, Clone, PartialEq)]
pub enum DisplayType {
    Block,
    Inline,
    InlineBlock,
    Flex,
    Grid,
    None,
}

impl Default for ComputedStyle {
    fn default() -> Self {
        Self {
            color: None,
            background_color: None,
            font_size: None,
            font_weight: None,
            border_width: None,
            border_color: None,
            layout_style: Style::default(),
            display: DisplayType::Block,
        }
    }
}

impl ComputedStyle {
    pub fn apply_declaration(&mut self, declaration: &Declaration) {
        match declaration.property.as_str() {
            "color" => {
                self.color = self.parse_color_value(&declaration.value);
            }
            "background-color" => {
                self.background_color = self.parse_color_value(&declaration.value);
            }
            "font-size" => {
                self.font_size = self.parse_length_value(&declaration.value);
            }
            "font-weight" => {
                self.font_weight = Some(declaration.value.clone());
            }
            "border-width" => {
                self.border_width = self.parse_length_value(&declaration.value);
            }
            "border-color" => {
                self.border_color = self.parse_color_value(&declaration.value);
            }
            "display" => {
                self.display = self.parse_display(&declaration.value);
                // Update Taffy layout style
                self.layout_style.display = match self.display {
                    DisplayType::Block => Display::Block,
                    DisplayType::Inline => Display::Block, // Taffy doesn't have inline
                    DisplayType::InlineBlock => Display::Block,
                    DisplayType::Flex => Display::Flex,
                    DisplayType::Grid => Display::Grid,
                    DisplayType::None => Display::None,
                };
            }
            "flex-direction" => {
                self.layout_style.flex_direction = match declaration.value.as_str() {
                    "row" => FlexDirection::Row,
                    "column" => FlexDirection::Column,
                    "row-reverse" => FlexDirection::RowReverse,
                    "column-reverse" => FlexDirection::ColumnReverse,
                    _ => FlexDirection::Row,
                };
            }
            "align-items" => {
                self.layout_style.align_items = Some(match declaration.value.as_str() {
                    "flex-start" => AlignItems::FlexStart,
                    "flex-end" => AlignItems::FlexEnd,
                    "center" => AlignItems::Center,
                    "stretch" => AlignItems::Stretch,
                    _ => AlignItems::Stretch,
                });
            }
            "justify-content" => {
                self.layout_style.justify_content = Some(match declaration.value.as_str() {
                    "flex-start" => JustifyContent::FlexStart,
                    "flex-end" => JustifyContent::FlexEnd,
                    "center" => JustifyContent::Center,
                    "space-between" => JustifyContent::SpaceBetween,
                    "space-around" => JustifyContent::SpaceAround,
                    _ => JustifyContent::FlexStart,
                });
            }
            _ => {
                // Log unsupported properties for debugging
                println!("Unsupported CSS property: {}", declaration.property);
            }
        }
    }

    /// Parse a CSS color value into ColorValue enum
    fn parse_color_value(&self, value: &str) -> Option<ColorValue> {
        let value = value.trim();

        // Named colors
        if matches!(
            value,
            "red"
                | "green"
                | "blue"
                | "black"
                | "white"
                | "transparent"
                | "yellow"
                | "cyan"
                | "magenta"
                | "gray"
                | "grey"
        ) {
            return Some(ColorValue::Named(value.to_string()));
        }

        // Hex colors
        if value.starts_with('#') && (value.len() == 4 || value.len() == 7) {
            return Some(ColorValue::Hex(value[1..].to_string()));
        }

        // RGB colors (simplified)
        if value.starts_with("rgb(") && value.ends_with(")") {
            let rgb_str = &value[4..value.len() - 1];
            let parts: Vec<&str> = rgb_str.split(',').collect();
            if parts.len() == 3 {
                if let (Ok(r), Ok(g), Ok(b)) = (
                    parts[0].trim().parse::<u8>(),
                    parts[1].trim().parse::<u8>(),
                    parts[2].trim().parse::<u8>(),
                ) {
                    return Some(ColorValue::Rgb(r, g, b));
                }
            }
        }

        None
    }

    /// Parse a CSS length value into LengthValue enum
    fn parse_length_value(&self, value: &str) -> Option<LengthValue> {
        let value = value.trim();

        if value.ends_with("px") {
            if let Ok(px) = value[..value.len() - 2].parse::<f32>() {
                return Some(LengthValue::Px(px));
            }
        } else if value.ends_with("em") {
            if let Ok(em) = value[..value.len() - 2].parse::<f32>() {
                return Some(LengthValue::Em(em));
            }
        } else if value.ends_with("%") {
            if let Ok(pct) = value[..value.len() - 1].parse::<f32>() {
                return Some(LengthValue::Percent(pct));
            }
        } else if let Ok(px) = value.parse::<f32>() {
            // Assume unitless values are pixels
            return Some(LengthValue::Px(px));
        }

        None
    }

    /// Parse a CSS display value
    fn parse_display(&self, value: &str) -> DisplayType {
        match value.trim() {
            "block" => DisplayType::Block,
            "inline" => DisplayType::Inline,
            "inline-block" => DisplayType::InlineBlock,
            "flex" => DisplayType::Flex,
            "grid" => DisplayType::Grid,
            "none" => DisplayType::None,
            _ => DisplayType::Block, // Default
        }
    }
}
