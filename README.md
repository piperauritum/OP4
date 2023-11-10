# OP4

## Overview
An attempt to generate Synthdef by Takumi Ikeda, which restoring OPM (YM2151) sounds using [timbre data for X68000](https://stdkmd.net/toypiano/controller.js-16-19.htm). OPM is YAMAHA's 4-operator FM synthesis sound chip.

OP4 is a 4-operator phase modulation oscillator class for SuperCollider, and the timbre data is edited by the OP4Editor, which follows the console design of [VOPMex](http://picopicose.com/software.html).

The parameters for the OPM are converted to actual values (amplitude, milliseconds, etc.) and displayed, except for the key-scaling and the irrational ratio (detune 2). The original parameters are not intuitive, e.g. 0 is the maximum value for some parameters. The mapping of the parameters to decay times, etc., uses the equation of the approximate curve obtained with LibreOffice Calc from the measurements of the VOPMex output.

Experimentally, the Wav knobs specify 8 types of waveforms derived from the OPZ (YM2414). Please note that the author owns neither actual OPM nor OPZ and is trying to restore the sounds only from information on the web, and the restored sounds are different from the VOPMex sound.

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