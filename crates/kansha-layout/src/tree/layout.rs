use kansha_interface::font::{FontBlob, Glyph};
use kansha_interface::geo::{Point, Rect, Size, SizeU32};
use std::fmt::Debug;

/// Trait that defines all layout information of a node. Currently residing in the same tree that also
/// holds the RenderTree. This is not ideal, but it is a start.
pub trait Layout: Default + Debug {
    /// Returns the relative upper left pos of the content box
    fn rel_pos(&self) -> Point;

    /// Returns the z-index of the element
    fn z_index(&self) -> u32;

    /// Size of the scroll box (content box without overflow), including scrollbars (if any)
    fn size(&self) -> Size;
    fn size_or(&self) -> Option<Size>;

    fn set_size_and_content(&mut self, size: SizeU32) {
        self.set_size(size);
        self.set_content(size);
    }
    fn set_size(&mut self, size: SizeU32);
    fn set_content(&mut self, size: SizeU32);

    /// Size of the content box (content without scrollbars, but with overflow)
    fn content(&self) -> Size;
    fn content_box(&self) -> Rect {
        let pos = self.rel_pos();
        let size = self.size();
        Rect::from_components(pos, size)
    }

    /// Additional space taken up by the scrollbar
    fn scrollbar(&self) -> Size;
    fn scrollbar_box(&self) -> Rect {
        let pos = self.rel_pos();
        let content = self.content();
        let size = self.scrollbar();
        Rect::new(
            pos.x,
            pos.y,
            content.width + size.width,
            content.height + size.height,
        )
    }

    fn border(&self) -> Rect;
    fn border_box(&self) -> Rect {
        let pos = self.rel_pos();
        let size = self.size();
        let border = self.border();

        Rect::new(
            pos.x - border.x1,
            pos.y - border.y1,
            size.width + border.x2,
            size.height + border.y2,
        )
    }

    fn padding(&self) -> Rect;
    fn padding_box(&self) -> Rect {
        let pos = self.rel_pos();
        let border = self.border();
        Rect::from_components(pos, border.size())
    }

    fn margin(&self) -> Rect;
    fn margin_box(&self) -> Rect {
        let border = self.border_box();
        let margin = self.margin();

        Rect::new(
            border.x1 - margin.x1,
            border.y1 - margin.y1,
            border.x2 + margin.x2,
            border.y2 + margin.y2,
        )
    }
}

/// Text layout that keeps all information on how a part of text is laid out
pub trait TextLayout {
    /// Returns a list of glyphs for the text
    fn glyphs(&self) -> &[Glyph];
    /// Font data
    fn font_data(&self) -> &FontBlob;
    // Size of the font in pixels
    fn font_size(&self) -> f32;
    /// Additional font decorations
    fn decorations(&self) -> &Decoration;
    // Offset?
    fn offset(&self) -> Point;
    /// Coordinates of the font
    fn coords(&self) -> &[i16];
    /// Size of the text
    fn size(&self) -> Size;
}

#[derive(Debug, Clone, Default)]
pub struct Decoration {
    pub underline: bool,
    pub overline: bool,
    pub line_through: bool,

    pub color: (f32, f32, f32, f32),
    pub style: DecorationStyle,
    pub width: f32,

    pub underline_offset: f32,

    pub x_offset: f32,
}

#[derive(Debug, Clone, Default)]
pub enum DecorationStyle {
    #[default]
    Solid,
    Double,
    Dotted,
    Dashed,
    Wavy,
}
