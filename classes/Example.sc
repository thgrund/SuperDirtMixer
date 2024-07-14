
// Service Classes
ServiceA {
    var handler;

    *new { |handler|
        ^super.newCopyArgs(handler);
    }

    handleEvent { |eventName, eventData|
        ("ServiceA received % with data: %".format(eventName, eventData)).postln;
    }
}

ServiceB {
    var handler;

    *new { |handler|
        ^super.newCopyArgs(handler);
    }

    handleEvent { |eventName, eventData|
        ("ServiceB received % with data: %".format(eventName, eventData)).postln;

        // Triggering another event from ServiceB
        if (eventName == \eventB) {
            handler.emitEvent(\eventD, "Data for Event D from ServiceB");
        }
    }
}

ServiceC {
    var handler;

    *new { |handler|
        ^super.newCopyArgs(handler);
    }

    handleEvent { |eventName, eventData|
        ("ServiceC received % with data: %".format(eventName, eventData)).postln;
    }
}