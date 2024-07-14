UIFactories {
	classvar <>formaters; // Define a class variable with getter and setter

	*new {
        formaters = Dictionary.newFrom([
            \toDecibels, {|value| "%dB".format(value.ampdb.round(1e-1))},
            \toMilliseconds, {|value| "%ms".format(value.linlin(0,1,0,10).round(1e-2))},
            \toFreqdB, {|value| "%dB".format(value.linlin(0,1,-24,24).round(1e-2))}
        ]);

		^super.new.init;
    }

}