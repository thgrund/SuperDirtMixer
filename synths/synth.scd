(
{
var numChannels = ~dirt.numChannels;

SynthDef("dirt_global_eq" ++ numChannels, { |out, dryBus, effectBus, gate = 1|
		var sound;
		var in = In.ar(dryBus, numChannels);
		var env = EnvGen.kr(Env.asr, gate, doneAction:1);

		// Dry/Wet
		// TODO: disable if no value was received
		sound = in.equi;

		DirtPause.ar(sound, graceTime: 4);

		sound = sound * env;

		ReplaceOut.ar(dryBus, sound);

	}, [\ir]).add;

	// Adapted from// https://github.com/alikthename/Musical-Design-in-Supercollider
	// https://www.youtube.com/watch?v=CAJXgJpQuSM&list=PLXCUkMwOEWQtB-leHHSexTizzcACdozp9&index=16
SynthDef("dirt_global_compressor" ++ numChannels, { |out, dryBus, effectBus, gate = 1, orbitIndex,
	cpAttack=0.01, cpRelease=0.1, cpThresh= -6, cpTrim=0, cpGain=0, cpRatio=4,
	cpLookahead=0, cpSaturate=1, cpHpf=50, cpKnee=0, cpBias=0 , activeCompressor = 0|

	var local, dry, dryAmplitude, in, t, o, r, gainReduction, e, cpLookaheadDelay, compressedSound;
	var cpKneelag;
	var env = EnvGen.kr(Env.asr, gate, doneAction:1);

	dry = In.ar(dryBus, numChannels);

	dryAmplitude = Amplitude.ar(dry, 0.001, 1.0);

	in = dry * cpTrim.dbamp;
	e = in.mean;
	e = HPF.ar(e, cpHpf);
	e = EnvDetect.ar(e, cpAttack, cpRelease);

	o = e.ampdb.excess(cpThresh);

	r = cpRatio;

	gainReduction = (max(o, 0.0)) * (r.reciprocal - 1);
	cpKneelag = cpAttack * cpKnee;

	gainReduction = gainReduction.lag(cpKneelag);
	gainReduction = gainReduction.dbamp;

	cpLookaheadDelay = DelayC.ar(in, 0.5, (cpAttack + cpRelease + cpKneelag * cpLookahead).lag(0.4));

	compressedSound = cpLookaheadDelay * gainReduction;
	in = compressedSound * cpGain.dbamp;

	in = Select.ar(cpSaturate, [in, (in + K2A.ar(cpBias)).softclip - cpBias]);
	in = LeakDC.ar(in);

	in = in * env;
	DirtPause.ar(in, graceTime: 4);

	//SendReply.kr(Impulse.kr(4), '/compressor', [dryAmplitude, gainReduction, orbitIndex]);
	SendPeakRMS.kr(dry, replyRate: 16, peakLag: 3, cmdName:"/cpInRms", replyID: orbitIndex);
	SendPeakRMS.kr(compressedSound, replyRate: 16, peakLag: 3, cmdName:"/cpCompressedRms", replyID: orbitIndex);

	ReplaceOut.ar(dryBus, in);
}, [\ir]).add;

SynthDef("dirt_wet_control" ++ numChannels, { |out, dryBus, effectBus, gate = 1, activeEq = 0, activeCompressor = 0|
	var env = EnvGen.kr(Env.asr, gate);
	var dry = In.ar(dryBus, numChannels) * env;
	//ReplaceOut.ar(dryBus, dry * (1- activeCompressor ) * (1 - activeEq));
}, [\ir]).add;

SynthDef("dirt_master_mix" ++ numChannels, { |dryBus, effectBus, gate = 1, masterGain = 1, gainControlLag = 0.1, globalMasterGain = 1|
	var drySignal = In.ar(dryBus, numChannels);
	var wetSignal = In.ar(effectBus, numChannels);
	var env = EnvGen.kr(Env.asr, gate);
	drySignal = (drySignal * env * masterGain.lag(gainControlLag)) * globalMasterGain;
	wetSignal = (wetSignal * env * masterGain.lag(gainControlLag)) * globalMasterGain;
	DirtPause.ar(drySignal + wetSignal, graceTime:4);
	XOut.ar(dryBus, env, drySignal);
	XOut.ar(effectBus, env, wetSignal);
}, [\ir, \ir, \kr, \kr, \kr, \kr]).add;

SynthDef("stageMaster", {
	|out = 0, compThreshold = 0.7, highEndDb = 0.58, limiterLevel = 1.0|
	var input = In.ar(out, numChannels);

	// Remove DC offset
	input = Select.ar(CheckBadValues.ar(input, 0, 0), [input, DC.ar(0), DC.ar(0), input]);

	// Compressor
	input = Compander.ar(
		input,
		input,
		thresh: compThreshold,
		slopeBelow: 1.05,
		slopeAbove: 0.4,
		clampTime:  0.01,
		relaxTime:  0.01
	);

	// Hi-shelf EQ, for brightness
	input = BHiShelf.ar(
		input,
		freq: 2400.0,
		rs: 1.0,
		db: highEndDb
	);


	// Limiter
	input = Limiter.ar(input, limiterLevel);
	ReplaceOut.ar(out, input);
}).add;

SynthDef("masterGain", {
	|out = 0, masterGain = 2.0|
	var input = In.ar(out, numChannels);

	input = input * masterGain;
	ReplaceOut.ar(out, input);
}).add;

}.value

);