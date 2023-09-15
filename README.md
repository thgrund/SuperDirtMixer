# SuperDirtMixer

**WARNING** as long as the following change is not applied in SuperDirt, you will receive an error when you try to call the `gui` function: https://github.com/musikinformatik/SuperDirt/pull/295/files

This is a mixer ui was created for the sound engine **SuperDirt** https://github.com/musikinformatik/SuperDirt for the **TidalCycles** http://tidalcycles.org/ programming language. The main motivation of this mixer was to get rid of using a DAW like Ableton in a live coding setup, because the author just used it for mixing a live setup. This mixer helps to realize tonal depth, clean frequency separation and a stereo panorama and makes this easily accessible in SuperDirt. In general this mixer overwrites the default values of the orbits defaultParentEvents. This means i.e. that it allows to change the value for gain from 1.0 to a different value from 0 to 1.5. These defaultParentEvent values will be used until they get overwritten on the TidalCycles side.

For the EQ of an orbit, this plugin is using EQui (https://github.com/muellmusik/EQui). For this it's needed to add some additional values to the defaultParentEvent to store and change the EQui parameters which will be done during the initialisation of this mixer. The eq functionality was added as a global effect to every orbit. This allows you to change the eq parameters for every orbit and still use all the other filter like hpf or lpf.

## Requirements

- [TidalCycles](https://github.com/tidalcycles/Tidal) v1.9.4+
- [SuperDirt Quark](https://github.com/musikinformatik/SuperDirt) v1.7.3+
- [EQui Quark](https://github.com/muellmusik/EQui) no versioning (latest)
- (Optional) [VSTPlugin](https://github.com/Spacechild1/vstplugin) 0.5.4+

![mixer](HelpSource/Classes/mixer.png)

## How to install it

You can download the bundled zip from this GitHub repository, unpack it and install the Quark in SuperCollider. You can do this in SuperCollider via `Language -> Quarks -> Install a folder`.

## How to use it

Pass this in your startup file after you installed it:

```C
(
    s.waitForBoot {
        ~dirt = SuperDirt(2, s);
        ~dirt.start(57120, 0 ! 14);
        // More SuperDirt ...

        // Initialize the SuperDirtMixer
        ~mixer = SuperDirtMixer(~dirt, 6010);

        // You can adjust parameters before you use the ui
        ~mixer.orbitLabels = ["d1 - Lead", "d2 - Bass", "d3 - Key", "d4 - Pad"];
        ~mixer.enableMasterPeakRMS(0)
    }
)
```

And then you can render the gui with executing this in any SuperCollider file 
```c
~mixer.gui
```

For more in depth configuration options you can have a look into the helpfile. This is accessable in SuperCollider when your cursor is on `SuperDirtMixer` and you press i.e. `Command + D` on MacOS. 
