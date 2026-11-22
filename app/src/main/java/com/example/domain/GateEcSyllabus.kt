package com.example.domain

data class GateSection(
    val id: String,
    val code: String,
    val name: String,
    val shortName: String,
    val weightageHint: String
)

data class GateSubtopic(
    val id: String,
    val sectionId: String,
    val name: String,
    val keyConcepts: String
)

object GateEcSyllabus {
    val sections: List<GateSection> = listOf(
        GateSection("eng_math", "EM", "Engineering Mathematics", "Engg. Math", "~13%"),
        GateSection("networks_signals", "NS", "Networks, Signals & Systems", "Networks & Signals", "~15%"),
        GateSection("electronic_devices", "ED", "Electronic Devices", "EDC", "~10%"),
        GateSection("analog_circuits", "AC", "Analog Circuits", "Analog", "~10%"),
        GateSection("digital_circuits", "DC", "Digital Circuits", "Digital", "~10%"),
        GateSection("control_systems", "CS", "Control Systems", "Control", "~9%"),
        GateSection("communications", "CM", "Communications", "Comm.", "~10%"),
        GateSection("electromagnetics", "EMW", "Electromagnetics", "EMFT", "~8%"),
        GateSection("general_aptitude", "GA", "General Aptitude", "Aptitude", "15%")
    )

    val subtopics: List<GateSubtopic> = listOf(
        // Section 1: Engineering Mathematics
        GateSubtopic("em_linear_algebra", "eng_math", "Linear Algebra & Matrices", "Eigenvalues, eigenvectors, rank, Cayley-Hamilton, systems of linear equations"),
        GateSubtopic("em_calculus", "eng_math", "Calculus & Maxima/Minima", "Taylor series, partial derivatives, line/surface/volume integrals, Stokes & Gauss theorems"),
        GateSubtopic("em_diff_eq", "eng_math", "Differential Equations", "First & higher-order linear ODEs, Cauchy-Euler, initial & boundary value problems"),
        GateSubtopic("em_complex_prob", "eng_math", "Complex Analysis & Probability", "Cauchy-Riemann, residue theorem, Bayes theorem, Gaussian/Poisson/Exponential distributions"),

        // Section 2: Networks, Signals and Systems
        GateSubtopic("ns_network_theorems", "networks_signals", "Network Theorems & Graph Theory", "Thevenin, Norton, Superposition, Maximum Power Transfer, Reciprocity, nodal/mesh analysis"),
        GateSubtopic("ns_transients_twoport", "networks_signals", "Transients & Two-Port Networks", "RL/RC/RLC transient response, sinusoidal steady-state, Z/Y/h/ABCD parameters"),
        GateSubtopic("ns_lti_systems", "networks_signals", "Signals & LTI Systems", "Causality, stability, linearity, time-invariance, convolution, impulse response"),
        GateSubtopic("ns_transforms", "networks_signals", "Fourier, Laplace & Z-Transforms", "CTFT, DTFT, DFT, Laplace ROC, Z-transform ROC, sampling theorem"),

        // Section 3: Electronic Devices
        GateSubtopic("ed_semiconductors", "electronic_devices", "Semiconductor Physics", "Energy bands, intrinsic/extrinsic carriers, drift/diffusion, Einstein relation, continuity equation"),
        GateSubtopic("ed_pn_junction", "electronic_devices", "P-N Junction & Special Diodes", "Built-in potential, depletion width, Zener, tunnel, varactor, photodiode, LED, solar cell"),
        GateSubtopic("ed_bjt_moscap", "electronic_devices", "BJT & MOS Capacitor", "Early effect, Ebers-Moll, flat-band & threshold voltage, accumulation/depletion/inversion"),
        GateSubtopic("ed_mosfet", "electronic_devices", "MOSFET Characteristics", "I-V equations, pinch-off, channel length modulation, body effect, subthreshold conduction"),

        // Section 4: Analog Circuits
        GateSubtopic("ac_diode_circuits", "analog_circuits", "Diode Circuits & Rectifiers", "Clippers, clampers, half/full-wave rectifiers, ripple factor, Zener voltage regulators"),
        GateSubtopic("ac_bjt_mos_amp", "analog_circuits", "BJT & MOSFET Amplifiers", "Biasing, small-signal hybrid-pi & T models, CE/CB/CC/CS/CG/CD gain & impedance, frequency response"),
        GateSubtopic("ac_opamp", "analog_circuits", "Op-Amp Applications & Filters", "Inverting/non-inverting, summer, integrator, differentiator, Schmitt trigger, active filters"),
        GateSubtopic("ac_feedback_osc", "analog_circuits", "Feedback & Oscillators", "4 feedback topologies, Barkhausen criterion, RC phase-shift, Wien bridge, Colpitts/Hartley, 555 timer"),

        // Section 5: Digital Circuits
        GateSubtopic("dc_boolean_kmap", "digital_circuits", "Number Systems & Boolean Algebra", "2's complement, IEEE-754, K-map minimization, prime implicants, logic families CMOS/TTL"),
        GateSubtopic("dc_combinational", "digital_circuits", "Combinational Circuits", "MUX, DEMUX, encoders, decoders, adders, look-ahead carry, comparators, hazards"),
        GateSubtopic("dc_sequential_fsm", "digital_circuits", "Sequential Circuits & FSMs", "SR/JK/D/T flip-flops, synchronous/asynchronous counters, shift registers, Moore/Mealy FSMs, setup/hold time"),
        GateSubtopic("dc_adc_dac_mem", "digital_circuits", "Data Converters & Memories", "R-2R ladder DAC, Flash/SAR/Dual-slope ADC, ROM/SRAM/DRAM, basic 8085/computer architecture"),

        // Section 6: Control Systems
        GateSubtopic("cs_modeling_sfg", "control_systems", "Transfer Functions & Signal Flow Graphs", "Mason's gain formula, block diagram reduction, open/closed-loop sensitivity"),
        GateSubtopic("cs_time_routh", "control_systems", "Time-Domain & Routh-Hurwitz Stability", "Steady-state errors Kp/Kv/Ka, 2nd-order damping & overshoot, Routh array stability"),
        GateSubtopic("cs_root_bode_nyquist", "control_systems", "Root Locus, Bode & Nyquist Plots", "Breakaway points, asymptotes, gain/phase margin, Nyquist encirclement criterion"),
        GateSubtopic("cs_state_space", "control_systems", "Compensators & State-Space Analysis", "Lead/Lag compensators, state transition matrix, controllability & observability"),

        // Section 7: Communications
        GateSubtopic("cm_random_noise", "communications", "Random Processes & Noise", "PSD, autocorrelation, WSS, white noise, thermal noise, noise figure, SNR"),
        GateSubtopic("cm_analog_mod", "communications", "Analog Modulation (AM, FM, PM)", "DSB-SC, SSB, superheterodyne receiver, Carson's rule, modulation index, pre/de-emphasis"),
        GateSubtopic("cm_info_theory", "communications", "Information Theory & Coding", "Entropy, mutual information, Shannon-Hartley capacity, Huffman coding, Hamming/linear block codes"),
        GateSubtopic("cm_digital_mod", "communications", "Digital Modulation & Optimal Receivers", "PCM, DPCM, Delta modulation, BASK/BPSK/BFSK/QPSK/QAM, matched filter, BER, ISI & Nyquist criterion"),

        // Section 8: Electromagnetics
        GateSubtopic("emw_maxwell_waves", "electromagnetics", "Maxwell's Equations & Plane Waves", "Displacement current, boundary conditions, Poynting vector, skin depth, polarization, Brewster angle"),
        GateSubtopic("emw_transmission_lines", "electromagnetics", "Transmission Lines & Smith Chart", "Characteristic impedance, reflection coefficient, VSWR, quarter-wave transformer, stub matching"),
        GateSubtopic("emw_waveguides", "electromagnetics", "Waveguides & Cavity Resonators", "Rectangular waveguides TE/TM modes, cutoff frequency, group/phase velocity, dominant TE10 mode"),
        GateSubtopic("emw_antennas_fiber", "electromagnetics", "Antennas & Optical Fibers", "Radiation resistance, directivity, Friis transmission equation, dipole arrays, numerical aperture"),

        // Section 9: General Aptitude
        GateSubtopic("ga_verbal", "general_aptitude", "Verbal Aptitude & Reasoning", "Grammar, vocabulary, sentence completion, critical inference, reading comprehension"),
        GateSubtopic("ga_quantitative", "general_aptitude", "Quantitative Aptitude", "Ratios, percentages, permutations & combinations, exponents, sequences, data interpretation"),
        GateSubtopic("ga_analytical", "general_aptitude", "Analytical & Logical Deduction", "Syllogisms, seating/arrangement puzzles, number series, clock/calendar logic"),
        GateSubtopic("ga_spatial", "general_aptitude", "Spatial & Pattern Aptitude", "2D/3D rotations, paper folding, mirror symmetry, geometric area ratios")
    )

    private val sectionMap = sections.associateBy { it.id }
    private val subtopicMap = subtopics.associateBy { it.id }

    fun sectionById(id: String): GateSection = sectionMap[id] ?: sections.first()
    fun subtopicById(id: String): GateSubtopic = subtopicMap[id] ?: subtopics.first()
    fun subtopicsForSection(sectionId: String): List<GateSubtopic> =
        subtopics.filter { it.sectionId == sectionId }
}
