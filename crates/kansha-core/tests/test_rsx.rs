use dioxus::prelude::*;
use kansha_core::kansha;

fn app() -> Element {
    let disabled = false;

    rsx! {
        div { style: "text-align: center; margin: 20px; display: flex; flex-direction: column; align-items: center;",
            button {
                "click to "
                if disabled { "enable" } else { "disable" }
                " the lower button"
            }
            button { disabled, "lower button" }
        }
    }
}

#[test]
fn test_rsx_block() {
    let disabled = false;

    let element = rsx! {
        div { style: "text-align: center; margin: 20px; display: flex; flex-direction: column; align-items: center;",
            button {
                "click to "
                if disabled { "enable" } else { "disable" }
                " the lower button"
            }
            button { disabled, "lower button" }
        }
    };
    println!("{:?}", element);

    kansha(app)

}