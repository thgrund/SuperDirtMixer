UIKnobFactories {
	// Define the formaters as a class variable

		/* INITALIZE GUI FACTORIES */
	knobFactory {
			| parent, key, title, formater, default, label |
            parent.put(key, Dictionary.new);

		    parent[key].put(\title, StaticText.new.string_(title).minWidth_(100).maxHeight_(30).align_(\center));

		    parent[key].put(\value,label);

		    parent[key].put(\element,
			    Knob().value_(default).action_({|a|
				    parent[key][\value].string_(
					    formater.value(a.value);
				    )
		        })
		    );
		}

	 knobWithValueLabelFactory {
		arg parent, key, title, formater, default;

		this.knobFactory( parent, key, title, formater, default,
			StaticText.new.string_(formater.value(default)).minWidth_(100).maxHeight_(30).align_(\center)
		);
	 }

	 knobWithNumberBoxFactory {
			| parent, key, title, formater, default |

			this.knobFactory( parent, key, title, formater, default,
			StaticText.new.string_(formater.value(default)).minWidth_(100).maxHeight_(30).align_(\center)
			);
		}

	 knobWithoutLabelFactory {
			| parent, key, title, formater, default |

			this.knobFactory( parent, key, title, formater, default,
				nil
			);
		}


}