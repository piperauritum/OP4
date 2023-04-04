OP4 {
	classvar ksbuf, sigs;

	*ar {|algo, feedback, freqs, envs, kss, pch, levels, dur, bufnums, lfowav, lfofrq, pmd, amd, pms, ams, ames|
		var buf, egs, algM, mdx, op0, op1, op2, op3, lfoamp, lfopch;

		buf = bufnums.collect{|e| OPWaveform.bufnum[e] };

		freqs = freqs * OPWaveform.pmo(lfowav.asInteger, pms.asInteger, lfofrq, pmd);

		algM = this.algoMatrix(algo);
		mdx = algM[0..2] * 10;

		lfoamp = OPWaveform.amo(lfowav.asInteger, ams.asInteger, lfofrq, amd);
		egs = envs.collect{|e,i|
			var scale = Index.kr(OPWaveform.ksbufnum[kss[i]], pch);
			var gate = EnvGen.kr(Env.linen(0, dur, 0));
			var env = EnvGen.kr(e, gate, timeScale: scale) * levels[i];
			LinSelectX.kr(ames[i], [env, env * lfoamp]);
		};

		op0 = this.fbop(buf[0], freqs[0], feedback, LocalIn.ar * feedback, egs[0]);
		op1 = Osc.ar(buf[1], freqs[1], op0 * mdx[0], egs[1]);
		op2 = Osc.ar(buf[2], freqs[2], Mix([op0, op1] * mdx[1]), egs[2]);
		op3 = Osc.ar(buf[3], freqs[3], Mix([op0, op1, op2] * mdx[2]), egs[3]);
		LocalOut.ar(op0);

		^Mix([op0, op1, op2, op3] * algM[3]) / algM[3].sum;
	}

	*algoMatrix {|algN = 0|
		var alg = [
		//	op1,	op2,	op3,		out
		//	[0,		[0, 1],	[0, 1, 2],	[0, 1, 2, 3]] (input)
			[1, 	[0, 1],	[0, 0, 1],	[0, 0, 0, 1]],
			[0, 	[1, 1],	[0, 0, 1],	[0, 0, 0, 1]],
			[0, 	[0, 1],	[1, 0, 1],	[0, 0, 0, 1]],
			[1, 	[0, 0],	[0, 1, 1],	[0, 0, 0, 1]],
			[1, 	[0, 0],	[0, 0, 1],	[0, 1, 0, 1]],
			[1, 	[1, 0],	[1, 0, 0],	[0, 1, 1, 1]],
			[1, 	[0, 0],	[0, 0, 0],	[0, 1, 1, 1]],
			[0, 	[0, 0],	[0, 0, 0],	[1, 1, 1, 1]],
		];

		^alg[algN];
	}

	*fbop {|bufnum, freq, feedback, mod, eg|
		if ( bufnum == 0 )
		{ ^SinOscFB.ar(freq, feedback * eg, eg) }
		{ ^Osc.ar(bufnum, freq, mod, eg) };
	}

	*op_label { ^[\lev, \atk, \d1t, \d1lev, \d2t, \rel, \sca, \ovt, \fine, \irr, \ame, \wav] }
	*pg_label { ^[\algo, \fb, \mask, \lfowav, \lfofrq, \pmd, \amd, \pms, \ams] }

	*sdef {|name, prog, param|
		var dx, dicProg, op_mask;

		dx = param.flop;
		dx = [this.op_label, dx].flop.flatten(1);
		dx = Dictionary.newFrom(dx);

		dicProg = [this.pg_label, prog].flop.flatten(1);
		dicProg = Dictionary.newFrom(dicProg);

		op_mask = prog[2].asInteger.asBinaryDigits(4).reverse;

		SynthDef(name, {|dur, pch, dyn, amp, pan=0|
			var env, frq, sig, gte, msk, dne;
			env = Array.fill(4, {|i|
				Env(
					levels: [0, 1, dx[\d1lev][i], 0, 0],
					times: [dx[\atk][i], dx[\d1t][i], dx[\d2t][i], dx[\rel][i]],
					curve: [\sin, \cub, \cub, \cub],
					releaseNode: 3
				);
			});

			frq = Array.fill(4, {|i|
				var frq = (dx[\fine][i] / 100 + pch).midicps;
				var ovt = dx[\ovt][i];
				var irr = [1, 2**0.5, 2.5**0.5, 3**0.5][dx[\irr][i]];
				frq * ovt * irr;
			});

			sig = this.ar(
				algo: dicProg[\algo],
				feedback: dicProg[\fb],
				freqs: frq,
				envs: env,
				kss: dx[\sca],
				pch: pch,
				levels:  Array.fill(4, {|i|
					dx[\lev][i] * dyn * op_mask[i];
				}),
				dur: dur,
				bufnums: dx[\wav],
				lfowav: dicProg[\lfowav],
				lfofrq: dicProg[\lfofrq],
				pmd: dicProg[\pmd],
				amd: dicProg[\amd],
				pms: dicProg[\pms],
				ams: dicProg[\ams],
				ames: dx[\ame],
			);

			// Free itself when the sound is gone
			gte = env.collect{|e,i|
				var scale = Index.kr(OPWaveform.ksbufnum[dx[\sca][i]], pch);
				var gate = EnvGen.kr(Env.linen(0, dur, 0));
				EnvGen.kr(e, gate, timeScale: scale);
			};
			msk = this.algoMatrix(dicProg[\algo])[3];
			dne = msk.collect{|e,i| if (e == 1, { Done.kr(gte[i]) }, { 0 }) };
			FreeSelf.kr(InRange.kr(sum(dne), msk.sum, msk.sum));

			OffsetOut.ar(0, Pan2.ar(sig * amp, pan));
		}).add;
	}
}


OPWaveform {
	classvar buf, ksbuf, sigs;

	*bufs {
		^buf;
	}

	*bufnum {
		^this.bufs.collect{|e| e.bufnum };
	}

	*ksbufs {
		^ksbuf;
	}

	*ksbufnum {
		^this.ksbufs.collect{|e| e.bufnum };
	}

	*amo {|wav, ams, freq, depth|
		^LinSelectX.kr(depth, [DC.kr(1), this.lfoamp(wav, ams, freq)])
	}

	*pmo {|wav, ams, freq, depth|
		^LinSelectX.kr(depth, [DC.kr(1), this.lfopch(wav, ams, freq)])
	}

	*lfoamp {|wav, ams, freq|
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

	*lfopch {|wav, pms, freq|
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

				buf = Buffer.allocConsecutive(8, Server.local, 2048, 1);

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
					buf[i].sendCollection(sig.asWavetable);
				};

				// ==== OPM key-scale map ====

				ksbuf = Buffer.allocConsecutive(4, Server.local, 200, 1);

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
					ksbuf[i].sendCollection(sig);
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