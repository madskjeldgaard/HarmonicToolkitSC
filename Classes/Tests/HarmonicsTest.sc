HarmonicsTest1 : UnitTest {
	test_check_classname {
		var result = Harmonics.new;
		this.assert(result.class == Harmonics);
	}
}


HarmonicsTester {
	*new {
		^super.new.init();
	}

	init {
		HarmonicsTest1.run;
	}
}
