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

SynthDef("dirt_master_mix" ++ numChannels, { |dryBus, effectBus, gate = 1, masterGain = 1, gainControlLag = 0.1|
	var drySignal = In.ar(dryBus, numChannels);
	var wetSignal = In.ar(effectBus, numChannels);
	var env = EnvGen.kr(Env.asr, gate);
	drySignal = drySignal * masterGain.lag(gainControlLag);
	wetSignal = wetSignal * masterGain.lag(gainControlLag);
	DirtPause.ar(drySignal + wetSignal, graceTime:4);
	XOut.ar(dryBus, env, drySignal);
	XOut.ar(effectBus, env, wetSignal);
}, [\ir, \ir, \kr, \kr, \kr, \kr]).add;

}.value
);