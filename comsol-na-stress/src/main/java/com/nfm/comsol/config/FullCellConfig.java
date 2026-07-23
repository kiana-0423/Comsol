package com.nfm.comsol.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/** Configuration for the explicit-particle 3D representative cell. */
public final class FullCellConfig {
    private final String geometryClassification, anodeLength, separatorLength, cathodeLength;
    private final String width, height, negativeLargeRadius, negativeSmallRadius;
    private final String electrolyteInitialConcentration, electrolyteConductivity, electrolyteDiffusivity;
    private final double transferenceNumber, chargeTransferAlpha;
    private final String negativeCsmax;
    private final double negativeInitialX;
    private final String negativeDiffusivity, negativeYoungModulus;
    private final double negativePoissonRatio, negativeBeta;
    private final List<Double> negativeExchangeCurrentDensitySensitivity;
    private final String binderConductivity, binderYoungModulus;
    private final double binderPoissonRatio;
    private final String separatorYoungModulus;
    private final double separatorPoissonRatio;
    private final double anodePorosity, cathodePorosity, separatorPorosity;
    private final double anodeTortuosity, cathodeTortuosity, separatorTortuosity;
    private final Path negativeOcvCsv, negativeKineticsCsv, negativeDiffusivityCsv;
    private final Path electrolyteConductivityCsv;
    private final List<Double> chargeSnapshotVoltages, dischargeSnapshotVoltages, snapshotSocFractions;
    private final double chargeCutoffVoltage, dischargeCutoffVoltage;
    private final String meshMaxSize, meshMinSize;
    private final double voltageRmseLimit, provisionalVoltageRmseLimit;
    private final double capacityErrorLimit, massBalanceErrorLimit;
    private final double convergenceAverageConcentrationLimit, convergenceConcentrationDeltaLimit;
    private final double convergenceAverageStressLimit, convergenceStressP95Limit;
    private final String parameterStatus, parameterSource;
    private final double parameterUncertainty;
    private final Path parameterMetadataCsv, sourceFile;
    private final Properties snapshot;

    public FullCellConfig(
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
            Path parameterMetadataCsv, Path sourceFile, Properties snapshot) {
        this.geometryClassification = geometryClassification;
        this.anodeLength = anodeLength;
        this.separatorLength = separatorLength;
        this.cathodeLength = cathodeLength;
        this.width = width;
        this.height = height;
        this.negativeLargeRadius = negativeLargeRadius;
        this.negativeSmallRadius = negativeSmallRadius;
        this.electrolyteInitialConcentration = electrolyteInitialConcentration;
        this.electrolyteConductivity = electrolyteConductivity;
        this.electrolyteDiffusivity = electrolyteDiffusivity;
        this.transferenceNumber = transferenceNumber;
        this.chargeTransferAlpha = chargeTransferAlpha;
        this.negativeCsmax = negativeCsmax;
        this.negativeInitialX = negativeInitialX;
        this.negativeDiffusivity = negativeDiffusivity;
        this.negativeYoungModulus = negativeYoungModulus;
        this.negativePoissonRatio = negativePoissonRatio;
        this.negativeBeta = negativeBeta;
        this.negativeExchangeCurrentDensitySensitivity = negativeExchangeCurrentDensitySensitivity;
        this.binderConductivity = binderConductivity;
        this.binderYoungModulus = binderYoungModulus;
        this.binderPoissonRatio = binderPoissonRatio;
        this.separatorYoungModulus = separatorYoungModulus;
        this.separatorPoissonRatio = separatorPoissonRatio;
        this.anodePorosity = anodePorosity;
        this.cathodePorosity = cathodePorosity;
        this.separatorPorosity = separatorPorosity;
        this.anodeTortuosity = anodeTortuosity;
        this.cathodeTortuosity = cathodeTortuosity;
        this.separatorTortuosity = separatorTortuosity;
        this.negativeOcvCsv = negativeOcvCsv;
        this.negativeKineticsCsv = negativeKineticsCsv;
        this.negativeDiffusivityCsv = negativeDiffusivityCsv;
        this.electrolyteConductivityCsv = electrolyteConductivityCsv;
        this.chargeSnapshotVoltages = chargeSnapshotVoltages;
        this.dischargeSnapshotVoltages = dischargeSnapshotVoltages;
        this.snapshotSocFractions = snapshotSocFractions;
        this.chargeCutoffVoltage = chargeCutoffVoltage;
        this.dischargeCutoffVoltage = dischargeCutoffVoltage;
        this.meshMaxSize = meshMaxSize;
        this.meshMinSize = meshMinSize;
        this.voltageRmseLimit = voltageRmseLimit;
        this.provisionalVoltageRmseLimit = provisionalVoltageRmseLimit;
        this.capacityErrorLimit = capacityErrorLimit;
        this.massBalanceErrorLimit = massBalanceErrorLimit;
        this.convergenceAverageConcentrationLimit = convergenceAverageConcentrationLimit;
        this.convergenceConcentrationDeltaLimit = convergenceConcentrationDeltaLimit;
        this.convergenceAverageStressLimit = convergenceAverageStressLimit;
        this.convergenceStressP95Limit = convergenceStressP95Limit;
        this.parameterStatus = parameterStatus;
        this.parameterSource = parameterSource;
        this.parameterUncertainty = parameterUncertainty;
        this.parameterMetadataCsv = parameterMetadataCsv;
        this.sourceFile = sourceFile;
        this.snapshot = snapshot;
    }

    public String geometryClassification() { return geometryClassification; }
    public String anodeLength() { return anodeLength; }
    public String separatorLength() { return separatorLength; }
    public String cathodeLength() { return cathodeLength; }
    public String width() { return width; }
    public String height() { return height; }
    public String negativeLargeRadius() { return negativeLargeRadius; }
    public String negativeSmallRadius() { return negativeSmallRadius; }
    public String electrolyteInitialConcentration() { return electrolyteInitialConcentration; }
    public String electrolyteConductivity() { return electrolyteConductivity; }
    public String electrolyteDiffusivity() { return electrolyteDiffusivity; }
    public double transferenceNumber() { return transferenceNumber; }
    public double chargeTransferAlpha() { return chargeTransferAlpha; }
    public String negativeCsmax() { return negativeCsmax; }
    public double negativeInitialX() { return negativeInitialX; }
    public String negativeDiffusivity() { return negativeDiffusivity; }
    public String negativeYoungModulus() { return negativeYoungModulus; }
    public double negativePoissonRatio() { return negativePoissonRatio; }
    public double negativeBeta() { return negativeBeta; }
    public List<Double> negativeExchangeCurrentDensitySensitivity() {
        return negativeExchangeCurrentDensitySensitivity;
    }
    public String binderConductivity() { return binderConductivity; }
    public String binderYoungModulus() { return binderYoungModulus; }
    public double binderPoissonRatio() { return binderPoissonRatio; }
    public String separatorYoungModulus() { return separatorYoungModulus; }
    public double separatorPoissonRatio() { return separatorPoissonRatio; }
    public double anodePorosity() { return anodePorosity; }
    public double cathodePorosity() { return cathodePorosity; }
    public double separatorPorosity() { return separatorPorosity; }
    public double anodeTortuosity() { return anodeTortuosity; }
    public double cathodeTortuosity() { return cathodeTortuosity; }
    public double separatorTortuosity() { return separatorTortuosity; }
    public Path negativeOcvCsv() { return negativeOcvCsv; }
    public Path negativeKineticsCsv() { return negativeKineticsCsv; }
    public Path negativeDiffusivityCsv() { return negativeDiffusivityCsv; }
    public Path electrolyteConductivityCsv() { return electrolyteConductivityCsv; }
    public List<Double> chargeSnapshotVoltages() { return chargeSnapshotVoltages; }
    public List<Double> dischargeSnapshotVoltages() { return dischargeSnapshotVoltages; }
    public List<Double> snapshotSocFractions() { return snapshotSocFractions; }
    public double chargeCutoffVoltage() { return chargeCutoffVoltage; }
    public double dischargeCutoffVoltage() { return dischargeCutoffVoltage; }
    public String meshMaxSize() { return meshMaxSize; }
    public String meshMinSize() { return meshMinSize; }
    public double voltageRmseLimit() { return voltageRmseLimit; }
    public double provisionalVoltageRmseLimit() { return provisionalVoltageRmseLimit; }
    public double capacityErrorLimit() { return capacityErrorLimit; }
    public double massBalanceErrorLimit() { return massBalanceErrorLimit; }
    public double convergenceAverageConcentrationLimit() { return convergenceAverageConcentrationLimit; }
    public double convergenceConcentrationDeltaLimit() { return convergenceConcentrationDeltaLimit; }
    public double convergenceAverageStressLimit() { return convergenceAverageStressLimit; }
    public double convergenceStressP95Limit() { return convergenceStressP95Limit; }
    public String parameterStatus() { return parameterStatus; }
    public String parameterSource() { return parameterSource; }
    public double parameterUncertainty() { return parameterUncertainty; }
    public Path parameterMetadataCsv() { return parameterMetadataCsv; }
    public Path sourceFile() { return sourceFile; }
    public Properties snapshot() { return snapshot; }
}
