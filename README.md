# OP4

## Overview
An attempt to generate Synthdef which restoring OPM (YM2151) sounds using X68000 voice data. OPM is YAMAHA's 4-operator FM synthesis sound chip.

OP4 is a 4-operator phase modulation oscillator class for SuperCollider, and the timbre data is edited by the OP4Editor, which follows the console design of VOPMex.

Parameters for OPM are converted to actual values (amplitude, milliseconds, etc.) and displayed, except for key-scaling, irrational ratio (Detune 2). The original parameters are not intuitive, for example some parameters 0 means maximum. The mapping of parameters to decay times, etc., uses the equation of the approximate curve obtained with LibreOffice Calc from the measurements of the recordings with VOPMex.

Experimentally, the Wav knob specifies 8 types of waveforms derived from the OPZ (YM2414). Note that the author doesn't have a real OPM or OPZ and is trying to recover the sound from information on the web, and the restored sounds are different from the VOPMex sound.

## Usage
- Place the OP4/OP4 folder into Platform.userExtensionDir or Platform.systemExtensionDir
- Then launch SuperCollider and see Examples.scd

### References (all in Japanese)

https://achapi2718.blogspot.com/2019/12/vsti-vopmex-yamaha-ym2151-opm.html

http://picopicose.com/software.html

https://nfggames.com/X68000/Documentation/Zmusic/zmusic2.txt

https://nfggames.com/X68000/Documentation/Zmusic/zmusic3.txt

http://nrtdrv.sakura.ne.jp/index.cgi?page=OPM%B2%BB%BF%A7%C4%EA%B5%C1%BD%F1%BC%B0

https://oykenkyu.blogspot.com/2022/05/ym2151.html

https://archive.org/details/InsideX680001992/page/n5/mode/2up