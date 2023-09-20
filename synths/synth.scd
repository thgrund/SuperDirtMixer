(
{
var numChannels = ~dirt.numChannels;

SynthDef("dirt_global_eq" ++ numChannels, { |out, dryBus, gate = 1|
		var sound;
		var in = In.ar(dryBus, numChannels);
		//ReplaceOut.ar(dryBus, in * (1 - gobalCutoffDry));

		// Dry/Wet
		// TODO: disable if no value was received
		sound = in.equi;
		DirtPause.ar(sound, graceTime:4);

		//ReplaceOut.ar(dryBus, (in * (1 - reverbWet)) + (sound * reverbWet ));
	    ReplaceOut.ar(dryBus, sound);

	}, [\ir]).add;

}.value
);