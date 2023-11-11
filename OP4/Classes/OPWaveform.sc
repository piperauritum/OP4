OPWaveform {
	classvar waveBuf, keyScaleBuf, sigs;

	waveBufnum {
		^waveBuf.collect{|e| e.bufnum };
	}

	keyScaleBufnum {
		^keyScaleBuf.collect{|e| e.bufnum };
	}

	amo {|wav, ams, freq, depth|
		^LinSelectX.kr(depth, [DC.kr(1), this.lfoamp(wav, ams, freq)])
	}

	pmo {|wav, ams, freq, depth|
		^LinSelectX.kr(depth, [DC.kr(1), this.lfopch(wav, ams, freq)])
	}

	lfoamp {|wav, ams, freq|
		switch (wav)
		{ 0 } {
			switch (ams)
			{ 0 } { ^DC.kr(1) }
			{ 1 } { ^LFSaw.kr(freq, 0, 0.5, 0.5).lincurve(curve:2.5) }
			{ 2 } { ^LFSaw.kr(freq, 0, 0.5, 0.5).lincurve(curve:5) }
			{ 3 } { ^LFSaw.kr(freq, 0, 0.5, 0.5).lincurve(curve:10) }
		}
		{ 1 } {
			switch (ams)
			{ 0 } { ^DC.kr(1) }
			{ 1 } { ^LFPulse.kr(freq, 0, 0.5, 1 - dbamp(-24), dbamp(-24)) }
			{ 2 } { ^LFPulse.kr(freq, 0, 0.5, 1 - dbamp(-48), dbamp(-48)) }
			{ 3 } { ^LFPulse.kr(freq, 0, 0.5, 1, 0) }
		}
		{ 2 } {
			switch (ams)
			{ 0 } { ^DC.kr(1) }
			{ 1 } { ^LFTri.kr(freq, 0, 0.5, 0.5).lincurve(curve:2.5) }
			{ 2 } { ^LFTri.kr(freq, 0, 0.5, 0.5).lincurve(curve:5) }
			{ 3 } { ^LFTri.kr(freq, 0, 0.5, 0.5).lincurve(curve:10) }
		}
		{ 3 } {
			switch (ams)
			{ 0 } { ^DC.kr(1) }
			{ 1 } { ^LFNoise0.kr(freq, 1 - dbamp(-3), dbamp(-3)) }
			{ 2 } { ^LFNoise0.kr(freq, 1 - dbamp(-6), dbamp(-6)) }
			{ 3 } { ^LFNoise0.kr(freq, 0.5, 0.5) }
		}
	}

	lfopch {|wav, pms, freq|
		var pow = [99, 4, 3, 2, 1, 0, -2, -3][pms];
		var ratio = [1, -1].collect{|a| 2 ** ((a/(2 ** pow))/12) };
		var range = ratio[0] - ratio[1];
		switch (wav)
		{ 0 } { ^LFSaw.kr(freq, 0, range/2, range/2 + ratio[1]) }
		{ 1 } { ^LFPulse.kr(freq, 0, 0.5, range, ratio[1]) }
		{ 2 } { ^LFTri.kr(freq, 0, range/2, range/2 + ratio[1]) }
		{ 3 } { ^LFNoise0.kr(freq, range/2, range/2 + ratio[1]) }
	}

	*initClass {
		Server.local.waitForBoot {
			Routine {

				// ==== OPZ operator Waveforms ====
				// cf. https://wave.hatenablog.com/entry/2021/09/20/212800

				waveBuf = Buffer.allocConsecutive(8, Server.local, 2048, 1, bufnum: 100);	// bufnum 100 - 107

				sigs = Array.fill(8, {|i|
					var sig = Signal.newClear(1024);
					sig.waveFill({|x|
						switch (i)
						{ 0 } { sin(x) }
						{ 1 } { sin(x) ** 2 * sign(sin(x)) }
						{ 2 } { sin(x).clip(0, 1) }
						{ 3 } { sin(x).clip(0, 1) ** 2 }
						{ 4 } { sin(x*2) * sign(sin(x).clip(0, 1)) }
						{ 5 } { sin(x*2) ** 2 * sign(sin(x*2)) * sign(sin(x).clip(0, 1)) }
						{ 6 } { sin(x*2).abs * sign(sin(x).clip(0, 1)) }
						{ 7 } { sin(x*2) ** 2 * sign(sin(x).clip(0, 1)) };
					}, 0, 2pi);
					// defer{ sig.plot };
					sig;
				});

				1.wait;

				sigs.do{|sig, i|
					waveBuf[i].sendCollection(sig.asWavetable);
				};

				// ==== OPM key-scale map ====

				keyScaleBuf = Buffer.allocConsecutive(4, Server.local, 200, 1, bufnum: 108);	// bufnum 108 - 111

				sigs = Array.fill(4, {|j|
					var coef = [
						[1.908, -0.006],
						[2.193, -0.013],
						[2.567, -0.028],
						[3.552, -0.056],
					];
					var sig = Signal.newClear(200);
					sig.waveFill({|x, old, i|
						coef[j][0] * (coef[j][1] * i).exp;
					}, 0, 199);
					// defer{ sig.plot };
					sig;
				});

				1.wait;

				sigs.do{|sig, i|
					keyScaleBuf[i].sendCollection(sig);
				};
			}.play;
		};
	}
}


Metallic {
	*ar {|freqHi, freqLo, duty, num|
		var sig;

		sig = Mix.fill(num, {|n|
			var bw, fq;
			bw = freqHi.cpsmidi - freqLo.cpsmidi;
			fq = bw / (num - 1) * n + freqLo.cpsmidi;
			LFPulse.ar(fq.midicps, 0, duty, num.reciprocal);
		});

		^sig;
	}
}