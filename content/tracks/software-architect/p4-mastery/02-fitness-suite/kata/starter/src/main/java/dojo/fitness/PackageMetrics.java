package dojo.fitness;

/** The metrics of one package. Do not change it. */
public record PackageMetrics(String pkg, int afferent, int efferent, double instability, double abstractness,
                             double distance) {}
