package scroogecoin_validator;

import java.util.ArrayList;

import scroogecoin_validator.Transaction.Input;
import scroogecoin_validator.Transaction.Output;

public class TxHandler {

	public UTXOPool ledger;

	/**
	 * Creates a public ledger whose current UTXOPool (collection of unspent
	 * transaction outputs) is {@code utxoPool}. This should make a copy of utxoPool
	 * by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	public TxHandler(UTXOPool utxoPool) {
		ledger = new UTXOPool(utxoPool);
	}

	/**
	 * @return true if: (1) all outputs claimed by {@code tx} are in the current
	 *         UTXO pool, (2) the signatures on each input of {@code tx} are valid,
	 *         (3) no UTXO is claimed multiple times by {@code tx}, (4) all of
	 *         {@code tx}s output values are non-negative, and (5) the sum of
	 *         {@code tx}s input values is greater than or equal to the sum of its
	 *         output values; and false otherwise.
	 */
	public boolean isValidTx(Transaction tx) {
		ArrayList<Input> inputs = tx.getInputs();
		ArrayList<Output> outputs = tx.getOutputs();
		ArrayList<UTXO> claimedUTXOs = new ArrayList<UTXO>();
		
		double totalOutputValue = 0;
		double totalInputValue = 0;	
		
		// For each input, verify if output claimed is in the pool
		// If the input signature is valid
		// and if the output claimed by this input hasn't been claimed already.
		for (int j = 0; j < inputs.size(); j++) {
			Input currentInput = inputs.get(j);
			UTXO targetUTXO = new UTXO(currentInput.prevTxHash, currentInput.outputIndex);
			Output claimedOutput = ledger.getTxOutput(targetUTXO);
			boolean isValidSignature = Crypto.verifySignature(claimedOutput.address,tx.getRawTx(),currentInput.signature);
			
			if (isValidSignature & ledger.contains(targetUTXO) & !claimedUTXOs.contains(targetUTXO)) {
				Output txOutput = ledger.getTxOutput(targetUTXO);
				claimedUTXOs.add(targetUTXO);
				totalInputValue += txOutput.value;
			} else {
				return false;
			}
		}
		
		// get total value of new outputs and check if any output is negative
		for (int i = 0; i < outputs.size(); i++) {
			Output currentOutput = outputs.get(i);
			if (currentOutput.value < 0) {
				return false;
			}
			totalOutputValue += currentOutput.value;
		}
		
		return totalOutputValue <= totalInputValue;	
	}

	/**
	 * Handles each epoch by receiving an unordered array of proposed transactions,
	 * checking each transaction for correctness, returning a mutually valid array
	 * of accepted transactions, and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		ArrayList<Transaction> validTxs = new ArrayList<Transaction>();

		
		for (int i = 0; i < possibleTxs.length; i++) {
			Transaction currentTx = possibleTxs[i];
			if (isValidTx(currentTx)) {
				validTxs.add(currentTx);
				ArrayList<Input> inputs = currentTx.getInputs();
				for (int j = 0; j < inputs.size(); j++) {
					Input input = inputs.get(j);
					UTXO targetUTXO = new UTXO(input.prevTxHash, input.outputIndex);
					ledger.removeUTXO(targetUTXO);
				}
			}
		}
		
		return validTxs.toArray(new Transaction[0]);
	}

}
