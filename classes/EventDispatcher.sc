EventDispatcher {
    var events;

    *new {
        ^super.new.init;
    }

    init {
        events = IdentityDictionary.new;
    }

    // Method to subscribe to an event
    subscribe { |eventName, action|
        events[eventName] = events[eventName] ?? { Set.new };
        events[eventName].add(action);
    }

    // Method to emit an event
    emit { |eventName, eventData|
        events[eventName].do { |action|
            action.value(eventData);
        };
    }

	printEventNames {
		["Event Dispatcher event names", events.keys].postln;
	}
}

