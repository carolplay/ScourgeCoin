import java.util.HashSet;
import java.util.Set;

public class TxHandler {


    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    protected UTXOPool pool;

    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.pool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        double sum_input = 0, sum_output = 0;
        int nb_inputs = tx.numInputs();
        Set<UTXO> claimed = new HashSet<>();

        for (int idx = 0;idx < nb_inputs;idx++) {
            Transaction.Input input = tx.getInput(idx);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            if (claimed.contains(utxo)) return false;   // 3
            else claimed.add(utxo);
            if (!this.pool.contains(utxo)) return false;    // 1
            Transaction.Output spent_output = this.pool.getTxOutput(utxo);
            if (!Crypto.verifySignature(spent_output.address, tx.getRawDataToSign(idx), input.signature))
                return false;   // 2
            sum_input += spent_output.value;
        }

        for (Transaction.Output output: tx.getOutputs()) {
            if (output.value < 0) return false; // 4
            sum_output += output.value;
        }

        if (sum_input < sum_output) return false;   // 5
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        Set<Transaction> validated = new HashSet<>();

        for (Transaction tx: possibleTxs) {
            if (isValidTx(tx)) {
                validated.add(tx);
                for (Transaction.Input input : tx.getInputs()) {
                    this.pool.removeUTXO(new UTXO(input.prevTxHash, input.outputIndex));
                }
                for (int idx = 0;idx < tx.numOutputs();idx++) {
                    this.pool.addUTXO(new UTXO(tx.getHash(), idx), tx.getOutput(idx));
                }
            }
        }

        return validated.toArray(new Transaction[validated.size()]);
    }
}

