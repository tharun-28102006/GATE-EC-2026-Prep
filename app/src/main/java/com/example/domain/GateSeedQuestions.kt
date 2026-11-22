package com.example.domain

data class SeedQuestion(
    val id: String,
    val sectionId: String,
    val subtopicId: String,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val marks: Int,
    val negativeMarks: Double,
    val difficulty: String,
    val explanation: String,
    val keyFormula: String
)

/**
 * Curated offline starter pool covering all 9 GATE 2026 EC syllabus sections (18 distinct subtopics).
 * Guarantees zero cold-start delay and full offline readiness on first launch, while Firebase AI Logic
 * continuously generates fresh, unseen GATE 2026 EC questions whenever online.
 */
object GateSeedQuestions {
    val all: List<SeedQuestion> = listOf(
        SeedQuestion(
            id = "seed_em_01",
            sectionId = "eng_math",
            subtopicId = "em_linear_algebra",
            questionText = "A 3x3 real matrix A has eigenvalues 2, -1, and 3. What is the determinant of the matrix (A^2 + I), where I is the 3x3 identity matrix?",
            options = listOf("50", "100", "30", "25"),
            correctIndex = 1,
            marks = 1,
            negativeMarks = 0.33,
            difficulty = "Easy",
            explanation = "If λ is an eigenvalue of A, then λ^2 + 1 is an eigenvalue of (A^2 + I).\n\nStep 1: Compute eigenvalues of (A^2 + I):\n• For λ1 = 2 → 2^2 + 1 = 5\n• For λ2 = -1 → (-1)^2 + 1 = 2\n• For λ3 = 3 → 3^2 + 1 = 10\n\nStep 2: The determinant equals the product of eigenvalues:\ndet(A^2 + I) = 5 × 2 × 10 = 100.\n\nCommon trap: Adding 1 to det(A)^2 directly (which gives 37) is incorrect because det(A^2 + I) ≠ det(A^2) + det(I).",
            keyFormula = "λ(f(A)) = f(λ) ;  det(M) = Π λ_i"
        ),
        SeedQuestion(
            id = "seed_em_02",
            sectionId = "eng_math",
            subtopicId = "em_complex_prob",
            questionText = "Let X be a zero-mean Gaussian random variable with variance σ^2 = 4. If Y = 3X - 2, what are the mean and variance of Y respectively?",
            options = listOf("Mean = -2, Variance = 12", "Mean = -2, Variance = 36", "Mean = 0, Variance = 36", "Mean = -2, Variance = 34"),
            correctIndex = 1,
            marks = 1,
            negativeMarks = 0.33,
            difficulty = "Easy",
            explanation = "For a linear transformation Y = aX + b:\n\nStep 1: Mean scales and shifts linearly:\nE[Y] = 3·E[X] - 2 = 3(0) - 2 = -2.\n\nStep 2: Variance scales by a^2 and is unaffected by the constant shift b:\nVar(Y) = a^2 · Var(X) = 3^2 × 4 = 9 × 4 = 36.\n\nHence Mean = -2 and Variance = 36.",
            keyFormula = "E[aX + b] = aE[X] + b ;  Var(aX + b) = a² Var(X)"
        ),
        SeedQuestion(
            id = "seed_ns_01",
            sectionId = "networks_signals",
            subtopicId = "ns_network_theorems",
            questionText = "A sinusoidal voltage source with internal impedance Z_s = (40 + j30) Ω delivers power to a purely resistive load R_L. For maximum average power transfer to R_L, the required value of R_L is:",
            options = listOf("40 Ω", "30 Ω", "50 Ω", "70 Ω"),
            correctIndex = 2,
            marks = 1,
            negativeMarks = 0.33,
            difficulty = "Medium",
            explanation = "When the load is purely resistive (R_L only, with no reactive part to cancel X_s), maximum power is transferred when R_L equals the magnitude |Z_s| of the source impedance.\n\nStep 1: Compute |Z_s|:\n|Z_s| = √(R_s^2 + X_s^2) = √(40^2 + 30^2) = √(1600 + 900) = √2500 = 50 Ω.\n\nCommon trap: Choosing R_L = 40 Ω applies only when X_L = -30 Ω can also be tuned (conjugate matching).",
            keyFormula = "For purely resistive load: R_L = |Z_s| = √(R_s² + X_s²)"
        ),
        SeedQuestion(
            id = "seed_ns_02",
            sectionId = "networks_signals",
            subtopicId = "ns_transforms",
            questionText = "The Nyquist sampling rate for the real signal x(t) = sinc(200t) + sinc^2(150t), where sinc(u) = sin(πu)/(πu), is:",
            options = listOf("300 Hz", "400 Hz", "600 Hz", "350 Hz"),
            correctIndex = 0,
            marks = 2,
            negativeMarks = 0.67,
            difficulty = "Medium",
            explanation = "Recall that sinc(2Bt) has a rectangular spectrum bandlimited to f_max = B Hz.\n\nStep 1: For x1(t) = sinc(200t) = sinc(2 × 100 × t), maximum frequency f1 = 100 Hz.\nStep 2: For sinc(150t), f_a = 75 Hz. Squaring in time corresponds to convolution in frequency, which doubles the bandwidth:\nf2 = 2 × 75 Hz = 150 Hz.\nStep 3: Maximum frequency of x(t) = max(f1, f2) = max(100, 150) = 150 Hz.\nStep 4: Nyquist rate f_N = 2 × f_max = 2 × 150 = 300 Hz.",
            keyFormula = "sinc(2Bt) → BW = B ;  sinc²(2Bt) → BW = 2B ;  f_N = 2 f_max"
        ),
        SeedQuestion(
            id = "seed_ed_01",
            sectionId = "electronic_devices",
            subtopicId = "ed_semiconductors",
            questionText = "In a non-degenerate semiconductor at room temperature (T = 300 K, V_T = 26 mV), the electron mobility is μ_n = 1300 cm²/(V·s). What is the electron diffusion coefficient D_n?",
            options = listOf("26.0 cm²/s", "33.8 cm²/s", "50.0 cm²/s", "13.0 cm²/s"),
            correctIndex = 1,
            marks = 1,
            negativeMarks = 0.33,
            difficulty = "Easy",
            explanation = "By the Einstein relation for non-degenerate semiconductors in thermal equilibrium:\n\nD_n / μ_n = kT / q = V_T.\n\nStep 1: Substitute μ_n = 1300 cm²/(V·s) and V_T = 0.026 V:\nD_n = 1300 × 0.026 = 33.8 cm²/s.\n\nUnit check: [cm²/(V·s)] × [V] = cm²/s.",
            keyFormula = "D_n / μ_n = kT / q = V_T  (≈ 26 mV at 300 K)"
        ),
        SeedQuestion(
            id = "seed_ed_02",
            sectionId = "electronic_devices",
            subtopicId = "ed_mosfet",
            questionText = "An enhancement-mode n-channel MOSFET has threshold voltage V_TH = 1.0 V and process parameter μ_n C_ox (W/L) = 2 mA/V². If V_GS = 3.0 V and V_DS = 4.0 V (neglecting channel-length modulation), what is the drain current I_D?",
            options = listOf("2 mA", "4 mA", "8 mA", "6 mA"),
            correctIndex = 1,
            marks = 2,
            negativeMarks = 0.67,
            difficulty = "Medium",
            explanation = "Step 1: Determine the operating region.\nOverdrive voltage V_OV = V_GS - V_TH = 3.0 - 1.0 = 2.0 V.\nSince V_DS (4.0 V) ≥ V_OV (2.0 V), the MOSFET operates in Saturation.\n\nStep 2: Apply the saturation drain current equation:\nI_D = (1/2) · μ_n C_ox (W/L) · (V_GS - V_TH)^2\nI_D = 0.5 × 2 mA/V² × (2.0 V)^2 = 1 × 4 = 4 mA.\n\nCommon trap: Forgetting the 1/2 factor gives 8 mA.",
            keyFormula = "I_D(sat) = ½ μ_n C_ox (W/L) (V_GS - V_TH)²  for V_DS ≥ V_GS - V_TH"
        ),
        SeedQuestion(
            id = "seed_ac_01",
            sectionId = "analog_circuits",
            subtopicId = "ac_opamp",
            questionText = "An ideal operational amplifier is configured as a non-inverting amplifier with input resistor R1 = 10 kΩ (to ground) and feedback resistor R_f = 90 kΩ. If a sinusoidal input of 100 mV peak is applied to the non-inverting terminal, what is the peak output voltage?",
            options = listOf("0.9 V", "1.0 V", "-0.9 V", "-1.0 V"),
            correctIndex = 1,
            marks = 1,
            negativeMarks = 0.33,
            difficulty = "Easy",
            explanation = "For an ideal non-inverting op-amp configuration:\n\nStep 1: Voltage gain A_v = 1 + (R_f / R1) = 1 + (90 kΩ / 10 kΩ) = 1 + 9 = 10.\nStep 2: Output peak voltage V_out = A_v × V_in = 10 × 0.1 V = 1.0 V (in phase with input).\n\nCommon trap: Using R_f/R1 = 9 ignores the '+1' term of the non-inverting topology.",
            keyFormula = "A_v(non-inv) = 1 + R_f / R1"
        ),
        SeedQuestion(
            id = "seed_ac_02",
            sectionId = "analog_circuits",
            subtopicId = "ac_feedback_osc",
            questionText = "A negative-feedback amplifier has open-loop gain A = 1000 and feedback factor β = 0.009. If the open-loop gain drops by 20% due to temperature drift, the approximate percentage drop in closed-loop gain is:",
            options = listOf("20%", "10%", "2%", "0.2%"),
            correctIndex = 2,
            marks = 2,
            negativeMarks = 0.67,
            difficulty = "Medium",
            explanation = "Negative feedback desensitivity factor is D = 1 + Aβ.\n\nStep 1: Calculate loop gain Aβ = 1000 × 0.009 = 9.\nStep 2: Desensitivity factor D = 1 + 9 = 10.\nStep 3: Fractional change in closed-loop gain A_f:\n(dA_f / A_f) ≈ (1 / (1 + Aβ)) × (dA / A) = (1 / 10) × 20% = 2%.\n\nThus negative feedback reduces gain sensitivity by a factor of 10.",
            keyFormula = "dA_f / A_f = [1 / (1 + Aβ)] · (dA / A)"
        ),
        SeedQuestion(
            id = "seed_dc_01",
            sectionId = "digital_circuits",
            subtopicId = "dc_boolean_kmap",
            questionText = "The minimal Sum-of-Products (SOP) form of the Boolean function F(A, B, C) = Σm(0, 1, 2, 3, 5, 7) is:",
            options = listOf("A' + C", "A' + B", "A + C'", "B' + C"),
            correctIndex = 0,
            marks = 1,
            negativeMarks = 0.33,
            difficulty = "Easy",
            explanation = "Plot the 3-variable K-map for minterms {0, 1, 2, 3, 5, 7}:\n\nStep 1: Minterms {0, 1, 2, 3} cover the entire A = 0 row → simplifies to A'.\nStep 2: Minterms {1, 3, 5, 7} cover all odd minterms where C = 1 → simplifies to C.\nStep 3: Combining both prime implicants covers all 6 minterms: F = A' + C.",
            keyFormula = "Σm(0,1,2,3) = A' ;  Σm(1,3,5,7) = C  ⇒  F = A' + C"
        ),
        SeedQuestion(
            id = "seed_dc_02",
            sectionId = "digital_circuits",
            subtopicId = "dc_adc_dac_mem",
            questionText = "An 8-bit Flash Analog-to-Digital Converter (ADC) requires how many analog comparators?",
            options = listOf("8", "256", "255", "511"),
            correctIndex = 2,
            marks = 1,
            negativeMarks = 0.33,
            difficulty = "Easy",
            explanation = "An N-bit Flash (parallel) ADC divides the reference voltage into 2^N quantization levels using a resistor ladder of 2^N resistors and compares the input simultaneously against (2^N - 1) thresholds.\n\nStep 1: For N = 8:\nNumber of comparators = 2^8 - 1 = 256 - 1 = 255.\n\nNote: A Flash ADC is the fastest ADC (1 clock cycle conversion), at the cost of exponential hardware complexity.",
            keyFormula = "Flash ADC comparators = 2^N - 1"
        ),
        SeedQuestion(
            id = "seed_cs_01",
            sectionId = "control_systems",
            subtopicId = "cs_time_routh",
            questionText = "A unity-feedback control system has open-loop transfer function G(s) = 20 / [s(s + 2)(s + 5)]. What is the steady-state error e_ss for a unit ramp input r(t) = t·u(t)?",
            options = listOf("0", "0.5", "2.0", "∞"),
            correctIndex = 1,
            marks = 2,
            negativeMarks = 0.67,
            difficulty = "Medium",
            explanation = "Since G(s) has one pole at the origin (Type-1 system), a unit ramp input produces a finite, non-zero steady-state error governed by the velocity error constant K_v.\n\nStep 1: Compute K_v = lim(s→0) s·G(s):\nK_v = lim(s→0) 20 / [(s + 2)(s + 5)] = 20 / (2 × 5) = 2.\n\nStep 2: Steady-state error for unit ramp:\ne_ss = 1 / K_v = 1 / 2 = 0.5.",
            keyFormula = "K_v = lim(s→0) sG(s) ;  e_ss(unit ramp) = 1 / K_v"
        ),
        SeedQuestion(
            id = "seed_cs_02",
            sectionId = "control_systems",
            subtopicId = "cs_root_bode_nyquist",
            questionText = "For an open-loop transfer function G(s)H(s) = K / [s(s + 3)(s + 6)], the centroid (real-axis intersection) of the root-locus asymptotes is located at:",
            options = listOf("s = -3", "s = -4.5", "s = -2", "s = -6"),
            correctIndex = 0,
            marks = 1,
            negativeMarks = 0.33,
            difficulty = "Easy",
            explanation = "The centroid σ_a of the root-locus asymptotes is given by (Σ finite poles - Σ finite zeros) / (P - Z).\n\nStep 1: Finite poles are at 0, -3, and -6 (P = 3). Finite zeros: none (Z = 0).\nStep 2: σ_a = [(0 - 3 - 6) - 0] / (3 - 0) = -9 / 3 = -3.\n\nAsymptote angles are ±60° and 180° radiating from s = -3.",
            keyFormula = "σ_a = (Σ poles - Σ zeros) / (P - Z)"
        ),
        SeedQuestion(
            id = "seed_cm_01",
            sectionId = "communications",
            subtopicId = "cm_info_theory",
            questionText = "An AWGN communication channel has a bandwidth of 4 kHz and a Signal-to-Noise Ratio (SNR) of 15 (linear scale). According to the Shannon-Hartley theorem, the maximum error-free channel capacity is:",
            options = listOf("8 kbps", "12 kbps", "16 kbps", "32 kbps"),
            correctIndex = 2,
            marks = 1,
            negativeMarks = 0.33,
            difficulty = "Easy",
            explanation = "By the Shannon-Hartley channel capacity theorem:\nC = B · log2(1 + SNR).\n\nStep 1: Substitute B = 4 kHz and SNR = 15:\n1 + SNR = 1 + 15 = 16 = 2^4.\nStep 2: log2(16) = 4 bits/symbol.\nStep 3: C = 4 kHz × 4 = 16 kbps.",
            keyFormula = "C = B · log₂(1 + S/N)  bits/sec"
        ),
        SeedQuestion(
            id = "seed_cm_02",
            sectionId = "communications",
            subtopicId = "cm_analog_mod",
            questionText = "In a standard single-tone AM wave with modulation index μ = 0.8, if the unmodulated carrier power is P_c = 100 W, what is the total transmitted power P_t?",
            options = listOf("132 W", "164 W", "140 W", "116 W"),
            correctIndex = 0,
            marks = 1,
            negativeMarks = 0.33,
            difficulty = "Easy",
            explanation = "For standard AM with single-tone sinusoidal modulation:\nP_t = P_c · (1 + μ^2 / 2).\n\nStep 1: Compute μ^2 / 2 = (0.8)^2 / 2 = 0.64 / 2 = 0.32.\nStep 2: P_t = 100 × (1 + 0.32) = 132 W.\n\nNote: Each sideband carries P_c · μ^2 / 4 = 16 W (total sideband power = 32 W).",
            keyFormula = "P_t(AM) = P_c (1 + μ² / 2)"
        ),
        SeedQuestion(
            id = "seed_emw_01",
            sectionId = "electromagnetics",
            subtopicId = "emw_transmission_lines",
            questionText = "A lossless transmission line with characteristic impedance Z_0 = 50 Ω is terminated in a purely resistive load Z_L = 150 Ω. What is the Voltage Standing Wave Ratio (VSWR) on the line?",
            options = listOf("0.5", "2.0", "3.0", "1.5"),
            correctIndex = 2,
            marks = 1,
            negativeMarks = 0.33,
            difficulty = "Easy",
            explanation = "Step 1: Compute reflection coefficient Γ:\nΓ = (Z_L - Z_0) / (Z_L + Z_0) = (150 - 50) / (150 + 50) = 100 / 200 = 0.5.\n\nStep 2: Compute VSWR:\nVSWR = (1 + |Γ|) / (1 - |Γ|) = (1 + 0.5) / (1 - 0.5) = 1.5 / 0.5 = 3.0.\n\nShortcut for purely resistive Z_L > Z_0: VSWR = Z_L / Z_0 = 150 / 50 = 3.0.",
            keyFormula = "Γ = (Z_L - Z_0)/(Z_L + Z_0) ;  VSWR = (1 + |Γ|)/(1 - |Γ|)"
        ),
        SeedQuestion(
            id = "seed_emw_02",
            sectionId = "electromagnetics",
            subtopicId = "emw_waveguides",
            questionText = "An air-filled rectangular waveguide has inner dimensions a = 3 cm and b = 1.5 cm. What is the cutoff frequency of the dominant TE_10 mode? (Take c = 3 × 10^8 m/s)",
            options = listOf("2.5 GHz", "5.0 GHz", "10.0 GHz", "7.5 GHz"),
            correctIndex = 1,
            marks = 2,
            negativeMarks = 0.67,
            difficulty = "Medium",
            explanation = "For an air-filled rectangular waveguide with broad wall dimension a > b, the dominant mode is TE_10 (m = 1, n = 0).\n\nStep 1: Cutoff wavelength λ_c = 2a = 2 × 3 cm = 6 cm = 0.06 m.\nStep 2: Cutoff frequency f_c = c / (2a) = (3 × 10^8) / (0.06) = 5 × 10^9 Hz = 5.0 GHz.\n\nNote that the narrow dimension b = 1.5 cm does not affect the TE_10 cutoff frequency.",
            keyFormula = "f_c(TE_10) = c / (2a)"
        ),
        SeedQuestion(
            id = "seed_ga_01",
            sectionId = "general_aptitude",
            subtopicId = "ga_quantitative",
            questionText = "If the ratio of the ages of two candidates A and B is 3 : 5 today, and 6 years from now the ratio becomes 2 : 3, what is A's present age?",
            options = listOf("12 years", "18 years", "24 years", "30 years"),
            correctIndex = 1,
            marks = 1,
            negativeMarks = 0.33,
            difficulty = "Easy",
            explanation = "Let present ages of A and B be 3k and 5k.\n\nStep 1: After 6 years:\n(3k + 6) / (5k + 6) = 2 / 3.\nStep 2: Cross-multiply:\n3(3k + 6) = 2(5k + 6) ⇒ 9k + 18 = 10k + 12 ⇒ k = 6.\nStep 3: A's present age = 3k = 3 × 6 = 18 years.",
            keyFormula = "(3k + 6)/(5k + 6) = 2/3  ⇒  k = 6"
        ),
        SeedQuestion(
            id = "seed_ga_02",
            sectionId = "general_aptitude",
            subtopicId = "ga_analytical",
            questionText = "In a class of 60 students preparing for GATE, 38 study Signals, 32 study Analog, and 15 study both. How many students study neither Signals nor Analog?",
            options = listOf("5", "10", "15", "8"),
            correctIndex = 0,
            marks = 1,
            negativeMarks = 0.33,
            difficulty = "Easy",
            explanation = "By the Principle of Inclusion-Exclusion:\n\nStep 1: |S ∪ A| = |S| + |A| - |S ∩ A| = 38 + 32 - 15 = 55 students study at least one of the two subjects.\nStep 2: Students studying neither = Total - |S ∪ A| = 60 - 55 = 5.",
            keyFormula = "|Neither| = N - (|A| + |B| - |A ∩ B|)"
        )
    )
}
