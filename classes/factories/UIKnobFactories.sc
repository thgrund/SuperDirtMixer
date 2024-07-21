UIKnobFactories {
	// Define the formaters as a class variable

		/* INITALIZE GUI FACTORIES */
	knobFactory {
			| parent, key, title, formater, default, label, action |
            parent.put(key, Dictionary.new);

		    parent[key].put(\title, StaticText.new.string_(title).maxHeight_(30).align_(\center));

		    parent[key].put(\value,label);

		    parent[key].put(\element,
			    Knob().value_(default).action_({|a|
				     parent[key][\value].string_(
					formater.value(a.value);
				    );
				   if (action.isNil.not, {
					action.value(a);
				});
		        })
		    );
		}

	 knobWithValueLabelFactory {
		arg parent, key, title, formater, default, action;

		this.knobFactory( parent, key, title, formater, default,
			StaticText.new.string_(formater.value(default)).maxHeight_(30).align_(\center), action
		);
	 }

	 knobWithNumberBoxFactory {
			| parent, key, title, formater, default, action |

			this.knobFactory( parent, key, title, formater, default,
			StaticText.new.string_(formater.value(default)).maxHeight_(30).align_(\center), action
			);
		}

	 knobWithoutLabelFactory {
			| parent, key, title, formater, default, action |

			this.knobFactory( parent, key, title, formater, default,
				nil, action
			);
		}


}