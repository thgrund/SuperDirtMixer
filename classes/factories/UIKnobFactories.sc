UIKnobFactories {
	var >activeOrbit;
	// Define the formaters as a class variable

	*new { |initActiveOrbit|
        ^super.new.init(initActiveOrbit);
    }

	init { |initActiveOrbit|
		if (initActiveOrbit.isNil.not,{
			activeOrbit = initActiveOrbit;
		})
	}

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

	knobFactory2 {
			| parent, key, title, knobToOrbitValue, orbitToKnobValue, formater, label, extraAction |
            parent.put(key, Dictionary.new);

		    parent[key].put(\knobToOrbitValue, knobToOrbitValue);

		    parent[key].put(\orbitToKnobValue, orbitToKnobValue);

		    parent[key].put(\title, StaticText.new.string_(title).maxHeight_(30).align_(\center));

		    parent[key].put(\value, label.string_(formater.format(activeOrbit.get(key))));

		    parent[key].put(\element,
			    Knob().value_(orbitToKnobValue.value(activeOrbit.get(key))).action_({|a|
				   var orbitValue;
				   orbitValue = parent[key][\knobToOrbitValue].value(a.value);

				   if (activeOrbit.isNil.not, {
					  activeOrbit.set(key, orbitValue);
				   });

				   parent[key][\value].string_(formater.format(orbitValue));

				   if (extraAction.isNil.not, {
					   extraAction.value();
				   });
		        })
		    );
		}

	 knobWithValueLabelFactory2 {
		arg parent, key, title, knobToOrbitValue, orbitToKnobValue, format, extraAction;

		this.knobFactory2( parent, key, title, knobToOrbitValue, orbitToKnobValue, format,
			StaticText.new.string_().maxHeight_(30).align_(\center), extraAction
		);
	 }

	knobFactory3 {
			| parent, key, title, knobToSynthValue, synthToKnobValue, formater, label, default, action |
            parent.put(key, Dictionary.new);

		    parent[key].put(\knobToSynthValue, knobToSynthValue);

		    parent[key].put(\synthToKnobValue, synthToKnobValue);

		    parent[key].put(\title, StaticText.new.string_(title).maxHeight_(30).align_(\center));

		    parent[key].put(\value, label.string_(formater.format(knobToSynthValue.value(default))));

		    parent[key].put(\element,
			    Knob().value_(default).action_({|a|
				     action.value(a.value);
				     parent[key][\value].string_(formater.format(knobToSynthValue.value(a.value)));
		        })
		    );
		}

	 knobWithValueLabelFactory3 {
		arg parent, key, title, knobToSynthValue, synthToKnobValue, format, default, action;

		this.knobFactory3( parent, key, title, knobToSynthValue, synthToKnobValue, format,
			StaticText.new.string_().maxHeight_(30).align_(\center), default, action
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