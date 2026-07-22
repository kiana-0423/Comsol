package com.nfm.comsol.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/** Configuration for the explicit-particle three-dimensional representative cell. */
public record FullCellConfig(
        String geometryClassification, String anodeLength, String separatorLength, String cathodeLength,
        String width, String height,
        String negativeLargeRadius, String negativeSmallRadius,
        String electrolyteInitialConcentration, String electrolyteConductivity, String electrolyteDiffusivity,
        double transferenceNumber, double chargeTransferAlpha,
        String negativeCsmax, double negativeInitialX, String negativeDiffusivity,
        String negativeYoungModulus, double negativePoissonRatio, double negativeBeta,
        List<Double> negativeExchangeCurrentDensitySensitivity,
        String binderConductivity, String binderYoungModulus, double binderPoissonRatio,
        String separatorYoungModulus, double separatorPoissonRatio,
        double anodePorosity, double cathodePorosity, double separatorPorosity,
        double anodeTortuosity, double cathodeTortuosity, double separatorTortuosity,
        Path negativeOcvCsv, Path negativeKineticsCsv, Path negativeDiffusivityCsv,
        Path electrolyteConductivityCsv,
        List<Double> chargeSnapshotVoltages, List<Double> dischargeSnapshotVoltages,
        List<Double> snapshotSocFractions,
        double chargeCutoffVoltage, double dischargeCutoffVoltage,
        String meshMaxSize, String meshMinSize,
        double voltageRmseLimit, double provisionalVoltageRmseLimit,
        double capacityErrorLimit, double massBalanceErrorLimit,
        double convergenceAverageConcentrationLimit, double convergenceConcentrationDeltaLimit,
        double convergenceAverageStressLimit, double convergenceStressP95Limit,
        String parameterStatus, String parameterSource, double parameterUncertainty,
        Path parameterMetadataCsv,
        Path sourceFile, Properties snapshot) {
}
