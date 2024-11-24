UtilityUI {
	var fxs;
	var loadPreset, savePreset;
	var presetListView, presetFile, presetFiles, presetPath;
	var handler;
	var orbits;
	var defaultParentEvent;

	*new { | initHandler, initOrbits, initPresetPath, initDefaultParentEvent|
        ^super.new.init(initHandler, initOrbits, initPresetPath, initDefaultParentEvent)
    }

    init { |initHandler, initOrbits, initPresetPath, initDefaultParentEvent|
		handler = initHandler;
		orbits = initOrbits;
		defaultParentEvent = initDefaultParentEvent;
		presetPath = initPresetPath;
		presetFile = 'Default.json';

		if (handler.isNil.not, {
			handler.subscribe(this, \extendDefaultParentEvent);
		});

		if (presetFile.isNil.not, {

			this.prLoadPresetFiles;

			presetListView = ListView(nil,Rect(10,10,30,30))
			.items_(presetFiles)
			.action_({ arg sbs;
				presetFile = presetListView.items[sbs.value]; // .value returns the integer
			}).maxWidth_(150);

			presetListView.value = nil;

			this.addTidalvstPresetListener;
			this.addLoadPresetListener;
		});
	}

	handleEvent { |eventName, eventData|
		if (eventName == \extendDefaultParentEvent, {
			eventData.pairsDo { |key, val|
				defaultParentEvent.put(key, val)
			};
		});
    }

	prLoadPresetFiles { |path|
		var filePaths;
		path = path ?? { presetPath.resolveRelative };
		filePaths = pathMatch(standardizePath(path +/+ "*"));

		presetFiles = filePaths.collect { |filepath|
		   PathName.new(filepath).fileName;
		};
	}

	/* PRESET MANAGEMENT */
	loadPreset {
		var defaultEvents = JSONlib.convertToSC(File.readAllString((presetPath ++ presetFile).resolveRelative, "r"));

	    defaultEvents.do({
		     arg defaultEvent, index;

			defaultParentEvent.keysValuesDo({|key, val|
				if (defaultEvent.keys.includes(key) == false, {defaultEvent.put(key, val)})
			});

			 orbits[index].set(*defaultEvent.asPairs);
        });

	}

	savePreset {
		var orbitPresets = Array.new(orbits.size);
		var file = File((presetPath ++ presetFile).resolveRelative, "w");

		orbits.do({
	         arg orbit;
  			 var presetEvent = ();

			 defaultParentEvent.keys.do({|eventKey| presetEvent.put(eventKey, orbit.get(eventKey)) });

			 orbitPresets.add(presetEvent);
        });

		file.write(*JSONlib.convertToJSON(orbitPresets));
        file.close;
	}

	/* DEFINE PRESET UI */
    createUI {
	    ^VLayout(
			StaticText.new.string_("Preset").fixedHeight_(15).align_(\center),
				/*Button.new.states_([["Mute All", Color.black, Color.white], ["Unmute All", Color.white, Color.blue]])
			    .action_({
				    |view|
				    if(view.value == 0) { this.tidalNetAddr.sendMsg("/unmuteAll") };
				    if(view.value == 1) { this.tidalNetAddr.sendMsg("/muteAll") };
			     }),*/
		presetListView,
		Button.new.string_("Save Preset")
				.action_({
				    handler.emitEvent(\updateActiveOrbit);
				    this.savePreset;
				}),
		Button.new.string_("Load Preset")
			    .action_({
				    |view|
				    this.loadPreset;
				    handler.emitEvent(\updateUI);
			     }),
		20,
		Button.new.string_("Reset All")
			    .action_({
				    |view|
				    handler.emitEvent(\resetAll);
			    })
         )
    }


	addLoadPresetListener {
		OSCFunc ({|msg|
			{
				 var receivedPresetFile = msg[1];
				 var presetFilesAsSymbol = presetFiles.collect({|item| item.asSymbol});
				 presetFile = receivedPresetFile;

				 this.loadPreset;
				 presetListView.value = presetFilesAsSymbol.indexOf(receivedPresetFile.asSymbol);

				 handler.emitEvent(\updateUI);
			}.defer;
	    }, ("/SuperDirtMixer/loadPreset"), recvPort: 57120).fix;
	}


	addTidalvstPresetListener { OSCFunc ({|msg|
				{
				    var fxName = msg[1];
				    var preset = msg[2];

					var combinedDictionary = Dictionary.new;
				    var keys;

				    combinedDictionary.putPairs(~tidalvst.instruments);
				    combinedDictionary.putPairs(~tidalvst.fxs);

				    keys = combinedDictionary.keys();

				    if (keys.includes(fxName), {
					    ~tidalvst.loadPreset(fxName, preset);
				    });
			}.defer;
	    }, ("/SuperDirtMixer/tidalvstPreset"), recvPort: 57120).fix;
	}

}