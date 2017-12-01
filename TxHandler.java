import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.List;
import java.util.Set;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool utxoPoolool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
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
	  
	   double inputValues = 0.0;

	   UTXOPool pastUTXOs = new UTXOPool();
	
	   for (int i = 0; i < tx.numInputs(); i++) {
 		Transaction.Input in = tx.getInput(i);
 		UTXO tempUTXO = new UTXO(in.prevTxHash, in.outputIndex);
		Transaction.Output out = utxoPool.getTxOutput(tempUTXO);

//(1) If none outputs claimed by {tx} are in the current UTXO pool return false 
		if (!utxoPool.contains(tempUTXO)) 
			return false;
 
//(2) If none the signatures on each input of {tx} are valid return false 
		if (!Crypto.verifySignature(out.address, tx.getRawdataToSign(i), in.signature))
                	return false;

//(3)If UTXO is already claimed by {tx} return false
		if (pastUTXOs.contains(tempUTXO)) 
			return false;

		pastUTXOs.addUTXO(tempUTXO, out);

		inputValues += out.value;
            
	   }

	   double outputValues = 0.0;

	   for (Transaction.Output out : tx.getOutputs()) {

//(4)If all of {tx}s output values are negative return false
            if (out.value < 0) return false;
            outputValues += out.value;
           }

//(5)If the sum of {tx} input values is greater than or equal to the sum of its output values return true
        return inputValues >= outputValues;

    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * utxoPooldating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {

	//receiving an unordered array of proposed transactions
   	List<Transaction> transactions = new ArrayList<Transaction>();

        for (Transaction transaction : possibleTxs) {
	//checking each transaction for correctness
            if (isValidTx(transaction)) {

                transactions.add(transaction);

                for(int i = 0; i < transaction.getInputs().size(); i ++) {
                    Transaction.Input input = transaction.getInput(i);
		//Remove the UTXOs claimed in this transaction
                    utxoPool.removeUTXO(new UTXO(input.prevTxHash, input.outputIndex));
                }

                for(int i = 0; i < transaction.getOutputs().size(); i ++) {
                    UTXO utxo = new UTXO(transaction.getHash(), i);
                    Transaction.Output output = transaction.getOutput(i);

//Add UTXOs generated in this transaction                                                                                			    
		    utxoPool.addUTXO(utxo, output);
                }
            }
        }

        Transaction[] result = new Transaction[transactions.size()];
        for(int i = 0; i < result.length; i ++) {
            result[i] = transactions.get(i);
        }

//returning a mutually valid array of accepted transactions
        return result;
    }

}
