EventHandler {
    var dispatcher;
	var inDebugMode;

    *new { | debug = false |
        ^super.new.init;
    }

    init { | debug |
        dispatcher = EventDispatcher.new;
		inDebugMode = debug;
    }

    // Method to emit events
    emitEvent { |eventName, eventData|
		if (inDebugMode == true, {"Emitting event % with data: %".format(eventName, eventData).postln;});

        dispatcher.emit(eventName, eventData);
    }

    // Method to subscribe services to events
    subscribe { |service, eventName|
		if (inDebugMode == true, {"Subscribing service % to event %".format(service, eventName).postln;});

        dispatcher.subscribe(eventName, { |data|
			if (inDebugMode == true, {"Delegating event % with data: % to service %".format(eventName, data, service).postln;});

            service.handleEvent(eventName, data);
        });
    }

	printEventNames {
		dispatcher.printEventNames;
	}
}
