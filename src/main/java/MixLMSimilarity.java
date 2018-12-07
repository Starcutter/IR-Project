import org.apache.lucene.search.similarities.*;

public class MixLMSimilarity extends LMSimilarity {

    private float lambda;
    private static float eps = 0.00001f;

    public MixLMSimilarity(float lambda) {
        if (lambda > -eps && lambda < (1f + eps)) {
            this.lambda = lambda;
        } else {
            this.lambda = 1f;
        }
    }

    @Override
    protected float score(BasicStats stats, float freq, float docLen) {
        return ((1 - this.lambda) * ((LMStats)stats).getCollectionProbability()
                + this.lambda * (freq + 1f) / (docLen + 1f));
    }

    @Override
    public String getName() {
        return String.format("Mixture Model (%f)", this.lambda);
    }
}
