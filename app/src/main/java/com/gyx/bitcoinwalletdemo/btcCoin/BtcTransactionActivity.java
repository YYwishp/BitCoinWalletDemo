
package com.gyx.bitcoinwalletdemo.btcCoin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.gyx.bitcoinwalletdemo.BaseActivity;
import com.gyx.bitcoinwalletdemo.R;
import com.gyx.bitcoinwalletdemo.RequestUtils;
import com.gyx.bitcoinwalletdemo.bean.AddressBalanceBean;
import com.gyx.bitcoinwalletdemo.btcCoin.btc.Address;
import com.gyx.bitcoinwalletdemo.btcCoin.btc.BTCUtils;
import com.gyx.bitcoinwalletdemo.btcCoin.btc.BitcoinException;
import com.gyx.bitcoinwalletdemo.btcCoin.btc.KeyPair;
import com.gyx.bitcoinwalletdemo.btcCoin.btc.Transaction;
import com.gyx.bitcoinwalletdemo.btcCoin.btc.UnspentOutputInfo;
import com.gyx.bitcoinwalletdemo.callback.OnDownLoadListener;
import com.gyx.bitcoinwalletdemo.zxing.activity.CaptureActivity;
import com.gyx.bitcoinwalletdemo.zxing.utils.CommonUtil;

import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletExtension;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class BtcTransactionActivity extends BaseActivity implements View.OnClickListener {
	private TextView tvBalance;
	private TextView tvFromAddress;
	private EditText edToAddress;
	private Button btScanerAddress;
	private EditText edValue;
	private TextView tvUnspendoutputInfo;
	private EditText edVbytePrice;
	private TextView tvFee;
	private Button btCalculateFee;
	private Button btSend;
	private Button btSignRawtransaction;
	private TextView tvPrivateKey;
	//打开扫描界面请求码
	private static final int REQUEST_CODE = 0x01;
	private AsyncTask<Void, Void, KeyPair> decodePrivateKeyTask;
	private KeyPair verifiedKeyPairForTx;
	private EditText spendBtcTxEdit;

	private TextView tvTransactionInfo;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_btc_transaction);
		initView();
	}

	private void initView() {
		tvBalance = (TextView) findViewById(R.id.tv_balance);
		tvFromAddress = (TextView) findViewById(R.id.tv_from_address);
		tvPrivateKey = (TextView) findViewById(R.id.tv_private_key);
		edToAddress = (EditText) findViewById(R.id.ed_to_address);
		btScanerAddress = (Button) findViewById(R.id.bt_scaner_address);
		edValue = (EditText) findViewById(R.id.ed_value);
		tvUnspendoutputInfo = (TextView) findViewById(R.id.tv_unspendoutput_info);
		edVbytePrice = (EditText) findViewById(R.id.ed_vbyte_price);
		tvFee = (TextView) findViewById(R.id.tv_fee);
		btCalculateFee = (Button) findViewById(R.id.bt_calculate_fee);
		btSend = (Button) findViewById(R.id.bt_send);
		btSignRawtransaction = (Button) findViewById(R.id.bt_sign_rawtransaction);


		spendBtcTxEdit = (EditText) findViewById(R.id.spend_btc_tx);

		//交易信息
		tvTransactionInfo = (TextView) findViewById(R.id.tv_transaction_info);



		//
		btScanerAddress.setOnClickListener(this);
		btCalculateFee.setOnClickListener(this);
		btSignRawtransaction.setOnClickListener(this);
		btSend.setOnClickListener(this);
		//
		//获取自己的地址（需要异步），然后请求balance，
		getAddressAndBalance();
		edValue.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				onSendAmountChanged(s.toString());
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
	}

	private void getAddressAndBalance() {
		Observable.create(new ObservableOnSubscribe<Wallet>() {
			@Override
			public void subscribe(ObservableEmitter<Wallet> e) throws Exception {
				Wallet wallet = getWallet();
				if (wallet != null) {
					e.onNext(wallet);
				}
			}
		}).subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Observer<Wallet>() {
					@Override
					public void onSubscribe(Disposable d) {
					}

					@Override
					public void onNext(Wallet wallet) {
						//地址
						String addressString = wallet.currentReceiveAddress().toBase58();

//						String addressString = currentReceiveAddress.toBase58();
//						tvFromAddress.setText(addressString);
//						Log.e("比特币交易", "BTC地址：" + addressString);
						tvFromAddress.setText("mpyrycpAH41DwPe7xi4tNL7PU8Prb6GMeG");

						//私钥
						DeterministicKey deterministicKey = wallet.currentReceiveKey();
						TestNet3Params parameters = TestNet3Params.get();
						String privateKeyAsWiF = deterministicKey.getPrivateKeyAsWiF(parameters);
//						tvPrivateKey.setText(privateKeyAsWiF);
//						Log.e("比特币交易", "BTC私钥：" + privateKeyAsWiF);

						tvPrivateKey.setText("cScLvainpq7HLGN1LJ358C4nyapjjnrs9KgMWJLE24FHUVXd4ATn");
						//初始化balance
						initBalance("mpyrycpAH41DwPe7xi4tNL7PU8Prb6GMeG");
						//Unspend信息
//						initUnspendInfo("n3yZVW5pBHznFLqJJUKTMjVoWWxBnpY9Qj");

						//获取unspentList数据
						initUnspendInfo("mpyrycpAH41DwPe7xi4tNL7PU8Prb6GMeG");
					}

					@Override
					public void onError(Throwable e) {
					}

					@Override
					public void onComplete() {
					}
				});
	}

	private Wallet getWallet() {
		//从本地恢复钱包
		try {
			WalletExtension walletExtension = new WalletExtension() {
				@Override
				public String getWalletExtensionID() {
					return null;
				}

				@Override
				public boolean isWalletExtensionMandatory() {
					return false;
				}

				@Override
				public byte[] serializeWalletExtension() {
					return new byte[0];
				}

				@Override
				public void deserializeWalletExtension(Wallet containingWallet, byte[] data) throws Exception {
				}
			};
			File fireBtc = new File(getFilesDir(), "btctest");
			Wallet wallet = Wallet.loadFromFile(fireBtc, walletExtension);
			return wallet;
		} catch (UnreadableWalletException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 从BlockChain.info 获取balance
	 *
	 * @param addressString
	 */
	private void initBalance(String addressString) {
		RequestUtils.getInstance().getAddressBalance(addressString, new OnDownLoadListener<AddressBalanceBean>() {
			@Override
			public void onSuccess(AddressBalanceBean addressBalanceBean) {
				String final_balance = addressBalanceBean.getFinal_balance();
				tvBalance.setText(final_balance);
			}

			@Override
			public void onFailed(Exception e) {
			}
		});
	}

	/**
	 * 获取 UnspentList数据
	 *
	 *
	 * @param addressString
	 */
	private void initUnspendInfo(String addressString) {
		RequestUtils.getInstance().getUnspent(addressString, new OnDownLoadListener<String>() {
			@Override
			public void onSuccess(String addressBalanceBean) {
				tvUnspendoutputInfo.setText(addressBalanceBean);
			}

			@Override
			public void onFailed(Exception e) {
				tvUnspendoutputInfo.setText(e.getMessage());
			}
		});
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.bt_scaner_address:
				cameraTask();
				break;
			case R.id.bt_calculate_fee:
				break;
			case R.id.bt_send:
				break;
			//交易签名
			case R.id.bt_sign_rawtransaction:
				btcTransction();
				break;
		}
	}

	@SuppressLint("StaticFieldLeak")
	private void btcTransction() {
		//私钥
		final String privateKeyToDecode = tvPrivateKey.getText().toString().trim();
		if (!TextUtils.isEmpty(privateKeyToDecode)) {
			//
			decodePrivateKeyTask = new AsyncTask<Void, Void, KeyPair>() {
				int addressType;

				@Override
				protected void onPreExecute() {
					//地址类型
					addressType = Address.PUBLIC_KEY_TO_ADDRESS_LEGACY;
				}

				@Override
				protected KeyPair doInBackground(Void... params) {
					try {
						//false 地址类型
						boolean compressedPublicKeyForPaperWallets = false;
						//解码 私钥
						BTCUtils.PrivateKeyInfo privateKeyInfo = BTCUtils.decodePrivateKey(privateKeyToDecode, compressedPublicKeyForPaperWallets);
						if (privateKeyInfo != null) {
							return new KeyPair(privateKeyInfo, addressType);
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					return null;
				}

				@Override
				protected void onPostExecute(KeyPair keyPair) {
					super.onPostExecute(keyPair);
					verifiedKeyPairForTx = keyPair;


					decodePrivateKeyTask = null;
					//当私钥修改的时候
//					onKeyPairModify(false, keyPair, addressType);
					//
					analysisUnSpendInfo(tvUnspendoutputInfo.getText().toString().trim(), keyPair);
				}
			};
			decodePrivateKeyTask.execute();
		} else {
		}
	}

	private AsyncTask<Void, Void, ArrayList<UnspentOutputInfo>> decodeUnspentOutputsInfoTask;
	private ArrayList<UnspentOutputInfo> verifiedUnspentOutputsForTx;
	private long verifiedAmountToSendForTx;
	private static final long SEND_MAX = -1;
	private static final long AMOUNT_ERR = -2;
	@Nullable
	private AsyncTask<Void, Void, GenerateTransactionResult> generateTransactionTask;

	/**
	 * 解析Unspent数据
	 *
	 * @param unspentOutputsInfoStr
	 * @param keyPair
	 */
	@SuppressLint("StaticFieldLeak")
	public void analysisUnSpendInfo(String unspentOutputsInfoStr, KeyPair keyPair) {
		decodeUnspentOutputsInfoTask = new AsyncTask<Void, Void, ArrayList<UnspentOutputInfo>>() {
			/**
			 * stores if input is a json.
			 * from Future interface spec: "Memory consistency effects: Actions taken by the asynchronous computation happen-before actions following the corresponding Future.get() in another thread."
			 * it means it don't have to be volatile, because AsyncTask uses FutureTask to deliver result.
			 */
			boolean jsonInput;
			String jsonParseError;

			@Override
			protected ArrayList<UnspentOutputInfo> doInBackground(Void... params) {
				try {
					if (keyPair.address == null) {
						throw new RuntimeException("Address is null in decodeUnspentOutputsInfoTask");
					}
					byte[] outputScriptWeAreAbleToSpend = Transaction.Script.buildOutput(keyPair.address.addressString).bytes;
					ArrayList<UnspentOutputInfo> unspentOutputs = new ArrayList<>();
					//1. decode tx or json
					String txs = unspentOutputsInfoStr.trim();
					byte[] startBytes = txs.length() < 8 ? null : BTCUtils.fromHex(txs.substring(0, 8));
					if (startBytes != null && startBytes.length == 4) {
						String[] txList = txs.split("\\s+");
						for (String rawTxStr : txList) {
							rawTxStr = rawTxStr.trim();
							if (rawTxStr.length() > 0) {
								byte[] rawTx = BTCUtils.fromHex(rawTxStr);
								if (rawTx != null && rawTx.length > 0) {
									Transaction baseTx = Transaction.decodeTransaction(rawTx);
									byte[] rawTxReconstructed = baseTx.getBytes();
									if (!Arrays.equals(rawTxReconstructed, rawTx)) {
										throw new IllegalArgumentException("Unable to decode given transaction");
									}
									jsonInput = false;
									byte[] txHash = baseTx.hash();
									for (int outputIndex = 0; outputIndex < baseTx.outputs.length; outputIndex++) {
										Transaction.Output output = baseTx.outputs[outputIndex];
										if (Arrays.equals(outputScriptWeAreAbleToSpend, output.scriptPubKey.bytes)) {
											unspentOutputs.add(new UnspentOutputInfo(keyPair, txHash, output.scriptPubKey, output.value, outputIndex));
										}
									}
								}
							}
						}
					} else {
						String jsonStr = unspentOutputsInfoStr.replace((char) 160, ' ').trim();//remove nbsp
						if (!jsonStr.startsWith("{")) {
							jsonStr = "{" + jsonStr;
						}
						if (!jsonStr.endsWith("}")) {
							jsonStr += "}";
						}
						JSONObject jsonObject = new JSONObject(jsonStr);
						jsonInput = true;
						JSONArray unspentOutputsArray = jsonObject.getJSONArray("unspent_outputs");
						if (unspentOutputsArray == null) {
							jsonParseError = getString(R.string.json_err_no_unspent_outputs);
							return null;
						}
						for (int i = 0; i < unspentOutputsArray.length(); i++) {
							JSONObject unspentOutput = unspentOutputsArray.getJSONObject(i);
							byte[] txHash = BTCUtils.reverse(BTCUtils.fromHex(unspentOutput.getString("tx_hash")));
							Transaction.Script script = new Transaction.Script(BTCUtils.fromHex(unspentOutput.getString("script")));
							if (Arrays.equals(outputScriptWeAreAbleToSpend, script.bytes)) {
								long value = unspentOutput.getLong("value");
								int outputIndex = (int) unspentOutput.getLong("tx_output_n");
								unspentOutputs.add(new UnspentOutputInfo(keyPair, txHash, script, value, outputIndex));
							}
						}
					}
					jsonParseError = null;
					return unspentOutputs;
				} catch (Exception e) {
					jsonParseError = e.getMessage();
					return null;
				}
			}

			@Override
			protected void onPostExecute(ArrayList<UnspentOutputInfo> unspentOutputInfos) {
				verifiedUnspentOutputsForTx = unspentOutputInfos;
				if (unspentOutputInfos == null) {
					if (jsonInput && !TextUtils.isEmpty(jsonParseError)) {
						//rawTxToSpendErr.setText(getString(R.string.error_unable_to_decode_json_transaction, jsonParseError));
					} else {
						//rawTxToSpendErr.setText(R.string.error_unable_to_decode_transaction);
					}
				} else if (unspentOutputInfos.isEmpty()) {
					//rawTxToSpendErr.setText(getString(R.string.error_no_spendable_outputs_found, keyPair.address));
				} else {
					//rawTxToSpendErr.setText("");
					long availableAmount = 0;
					for (UnspentOutputInfo unspentOutputInfo : unspentOutputInfos) {
						availableAmount += unspentOutputInfo.value;
					}
					//显示hint，默认显示全部的金额
					edValue.setHint(BTCUtils.formatValue(availableAmount));
					if (TextUtils.isEmpty(edValue.getText().toString().trim())) {
						verifiedAmountToSendForTx = SEND_MAX;
					}
					//生成交易
					tryToGenerateSpendingTransaction();
				}
			}
		};
		decodeUnspentOutputsInfoTask.execute();
	}

	/**
	 * 金额变化
	 *
	 * @param amountStr 金额
	 */
	private void onSendAmountChanged(String amountStr) {
		TextView amountError = findViewById(R.id.err_amount);
		if (TextUtils.isEmpty(amountStr)) {
			verifiedAmountToSendForTx = SEND_MAX;
			amountError.setText("");
			tryToGenerateSpendingTransaction();
		} else {
			try {
				double requestedAmountToSendDouble = Double.parseDouble(amountStr);
				long requestedAmountToSend = (long) (requestedAmountToSendDouble * 1e8);
				if (requestedAmountToSendDouble > 0 && requestedAmountToSendDouble < 21000000 && requestedAmountToSend > 0) {
					verifiedAmountToSendForTx = requestedAmountToSend;
					amountError.setText("");
					tryToGenerateSpendingTransaction();
				} else {
					verifiedAmountToSendForTx = AMOUNT_ERR;
					amountError.setText(R.string.error_amount_parsing);
				}
			} catch (Exception e) {
				verifiedAmountToSendForTx = AMOUNT_ERR;
				amountError.setText(R.string.error_amount_parsing);
			}
		}
	}


	/**
	 * 生成交易结果的类
	 */
	static class GenerateTransactionResult {
		static final int ERROR_SOURCE_UNKNOWN = 0;
		static final int ERROR_SOURCE_INPUT_TX_FIELD = 1;
		static final int ERROR_SOURCE_ADDRESS_FIELD = 2;
		static final int HINT_FOR_ADDRESS_FIELD = 3;
		static final int ERROR_SOURCE_AMOUNT_FIELD = 4;

		final Transaction btcTx, bchTx;
		final String errorMessage;
		final int errorSource;
		final long fee;

		GenerateTransactionResult(String errorMessage, int errorSource) {
			btcTx = null;
			bchTx = null;
			this.errorMessage = errorMessage;
			this.errorSource = errorSource;
			fee = -1;
		}

		GenerateTransactionResult(Transaction btcTx, @Nullable Transaction bchTx, long fee) {
			this.btcTx = btcTx;
			this.bchTx = bchTx;
			errorMessage = null;
			errorSource = ERROR_SOURCE_UNKNOWN;
			this.fee = fee;
		}
	}
	/**
	 * 尝试生成支出交易
	 */
	@SuppressLint("StaticFieldLeak")
	private void tryToGenerateSpendingTransaction() {
		final ArrayList<UnspentOutputInfo> unspentOutputs = verifiedUnspentOutputsForTx;//unspent,Json 数据集合
		final String outputAddress = edToAddress.getText().toString().trim();//验证过的收币地址
		final long requestedAmountToSend = verifiedAmountToSendForTx; //经过计算后的要发送的金额
		final KeyPair keyPair = verifiedKeyPairForTx;
		if (unspentOutputs != null && !unspentOutputs.isEmpty() && !TextUtils.isEmpty(outputAddress) &&
				keyPair != null && keyPair.address != null && requestedAmountToSend >= SEND_MAX && requestedAmountToSend != 0
				&& !TextUtils.isEmpty(keyPair.address.addressString)) {
			//cancelAllRunningTasks();

			generateTransactionTask = new AsyncTask<Void, Void, GenerateTransactionResult>() {

				@Override
				protected GenerateTransactionResult doInBackground(Void... voids) {
					Transaction btcSpendTx;
					Transaction bchSpendTx = null;
					try {
						//遍历unspentOutputs集合数据，获取可以消费的数量
						long availableAmount = 0;
						for (UnspentOutputInfo unspentOutputInfo : unspentOutputs) {
							availableAmount += unspentOutputInfo.value;
						}
						//判断可消费数量是否和发送数量一致，一致，就代表发送全部，不相等，就改成需要发送的数量
						long amount;
						if (availableAmount == requestedAmountToSend || requestedAmountToSend == SEND_MAX) {
							//transfer maximum possible amount
							amount = -1;
						} else {
							amount = requestedAmountToSend;
						}

						//每一个byte多少中本聪
						float satoshisPerVirtualByte = 50;


//						SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
//						try {
//							satoshisPerVirtualByte = preferences.getInt(PreferencesActivity.PREF_FEE_SAT_BYTE, FeePreference.PREF_FEE_SAT_BYTE_DEFAULT);
//						} catch (ClassCastException e) {
//							preferences.edit()
//									.remove(PreferencesActivity.PREF_FEE_SAT_BYTE)
//									.putInt(PreferencesActivity.PREF_FEE_SAT_BYTE, FeePreference.PREF_FEE_SAT_BYTE_DEFAULT)
//									.commit();
//							satoshisPerVirtualByte = FeePreference.PREF_FEE_SAT_BYTE_DEFAULT;
//						}


						//Always try to use segwit here even if it's disabled since the switch is only about generated address type
						//Do we need another switch to disable segwit in tx?
						btcSpendTx = BTCUtils.createTransaction(
								unspentOutputs,
								outputAddress,
								keyPair.address.addressString,
								amount,
								satoshisPerVirtualByte,//每一个byte多少中本聪
								BTCUtils.TRANSACTION_TYPE_SEGWIT//隔离验证
						);
						try {
							//解码收币地址
							Address outputAddressDecoded = Address.decode(outputAddress);
							if (outputAddressDecoded != null && outputAddressDecoded.keyhashType != Address.TYPE_P2SH) { //this check prevents sending BCH to SegWit
								bchSpendTx = BTCUtils.createTransaction(unspentOutputs,
										outputAddress, keyPair.address.addressString, amount, satoshisPerVirtualByte, BTCUtils.TRANSACTION_TYPE_BITCOIN_CASH);
							}
						} catch (Exception ignored) {
						}

						//6. double check that generated transaction is valid
						Transaction.Script[] relatedScripts = new Transaction.Script[btcSpendTx.inputs.length];
						long[] amounts = new long[btcSpendTx.inputs.length];
						for (int i = 0; i < btcSpendTx.inputs.length; i++) {
							Transaction.Input input = btcSpendTx.inputs[i];
							//验证输出，和输入
							for (UnspentOutputInfo unspentOutput : unspentOutputs) {
								if (Arrays.equals(unspentOutput.txHash, input.outPoint.hash) && unspentOutput.outputIndex == input.outPoint.index) {
									relatedScripts[i] = unspentOutput.scriptPubKey;
									amounts[i] = unspentOutput.value;
									break;
								}
							}
						}
						//验证btc交易
						BTCUtils.verify(relatedScripts, amounts, btcSpendTx, false);
						if (bchSpendTx != null) {
							//验证比特币现金交易
							BTCUtils.verify(relatedScripts, amounts, bchSpendTx, true);
						}
					} catch (BitcoinException e) {
						switch (e.errorCode) {
							case BitcoinException.ERR_INSUFFICIENT_FUNDS:
								return new GenerateTransactionResult(getString(R.string.error_not_enough_funds), GenerateTransactionResult.ERROR_SOURCE_AMOUNT_FIELD);
							case BitcoinException.ERR_FEE_IS_TOO_BIG:
								return new GenerateTransactionResult(getString(R.string.generated_tx_have_too_big_fee), GenerateTransactionResult.ERROR_SOURCE_INPUT_TX_FIELD);
							case BitcoinException.ERR_AMOUNT_TO_SEND_IS_LESS_THEN_ZERO:
								return new GenerateTransactionResult(getString(R.string.fee_is_greater_than_available_balance), GenerateTransactionResult.ERROR_SOURCE_INPUT_TX_FIELD);
							case BitcoinException.ERR_MEANINGLESS_OPERATION://input, output and change addresses are same.
								return new GenerateTransactionResult(getString(R.string.output_address_same_as_input), GenerateTransactionResult.ERROR_SOURCE_ADDRESS_FIELD);
//                            case BitcoinException.ERR_INCORRECT_PASSWORD
//                            case BitcoinException.ERR_WRONG_TYPE:
//                            case BitcoinException.ERR_FEE_IS_LESS_THEN_ZERO
//                            case BitcoinException.ERR_CHANGE_IS_LESS_THEN_ZERO
//                            case BitcoinException.ERR_AMOUNT_TO_SEND_IS_LESS_THEN_ZERO
							default:
								return new GenerateTransactionResult(getString(R.string.error_failed_to_create_transaction) + ": " + e.getMessage(), GenerateTransactionResult.ERROR_SOURCE_UNKNOWN);
						}
					} catch (Exception e) {
						return new GenerateTransactionResult(getString(R.string.error_failed_to_create_transaction) + ": " + e, GenerateTransactionResult.ERROR_SOURCE_UNKNOWN);
					}

					long inValue = 0;
					for (Transaction.Input input : btcSpendTx.inputs) {
						for (UnspentOutputInfo unspentOutput : unspentOutputs) {
							if (Arrays.equals(unspentOutput.txHash, input.outPoint.hash) && unspentOutput.outputIndex == input.outPoint.index) {
								inValue += unspentOutput.value;
							}
						}
					}
					long outValue = 0;
					for (Transaction.Output output : btcSpendTx.outputs) {
						outValue += output.value;
					}
					//矿工费
					long fee = inValue - outValue;
					return new GenerateTransactionResult(btcSpendTx, bchSpendTx, fee);
				}

				@Override
				protected void onPostExecute(GenerateTransactionResult result) {
					super.onPostExecute(result);
					generateTransactionTask = null;
					if (result != null) {
						final TextView rawTxToSpendError = findViewById(R.id.err_raw_tx);
						if (result.btcTx != null) {
							String amountStr = null;
							Transaction.Script out = null;
							try {
								out = Transaction.Script.buildOutput(outputAddress);
							} catch (BitcoinException ignore) {
							}
							if (result.btcTx.outputs[0].scriptPubKey.equals(out)) {
								amountStr = BTCUtils.formatValue(result.btcTx.outputs[0].value);
							}
							if (amountStr == null) {
								rawTxToSpendError.setText(R.string.error_unknown);
							} else {
								String feeStr = BTCUtils.formatValue(result.fee);
								//比特币旷工费用介绍
								getTxDescription(amountStr, result.btcTx.outputs, feeStr, false, keyPair, outputAddress);

								//SpannableStringBuilder descBuilderBch = result.bchTx == null ? null : getTxDescription(amountStr, result.bchTx.outputs, feeStr, true, keyPair, outputAddress);
								//矿工费介绍
//								spendBtcTxDescriptionView.setText(descBuilderBtc);
//								spendBtcTxDescriptionView.setVisibility(View.VISIBLE);
//								if (descBuilderBch != null) {
//									spendBchTxDescriptionView.setText(descBuilderBch);
//									spendBchTxDescriptionView.setVisibility(View.VISIBLE);
//								} else {
//									spendBchTxDescriptionView.setVisibility(View.GONE);
//								}
//								spendTxWarningView.setVisibility(View.VISIBLE);
								//签名信息
								spendBtcTxEdit.setText(BTCUtils.toHex(result.btcTx.getBytes()));
								Log.e("签名信息：", spendBtcTxEdit.getText().toString().trim());





//								spendBtcTxEdit.setVisibility(View.VISIBLE);
//								if (result.bchTx != null) {
//									spendBchTxEdit.setText(BTCUtils.toHex(result.bchTx.getBytes()));
//									spendBchTxEdit.setVisibility(View.VISIBLE);
//								} else {
//									spendBchTxEdit.setVisibility(View.GONE);
//								}
//								sendBtcTxInBrowserButton.setVisibility(View.VISIBLE);
//								sendBchTxInBrowserButton.setVisibility(result.bchTx != null ? View.VISIBLE : View.GONE);
							}
						} else if (result.errorSource == GenerateTransactionResult.ERROR_SOURCE_INPUT_TX_FIELD) {
							rawTxToSpendError.setText(result.errorMessage);
						} else if (result.errorSource == GenerateTransactionResult.ERROR_SOURCE_ADDRESS_FIELD ||
								result.errorSource == GenerateTransactionResult.HINT_FOR_ADDRESS_FIELD) {
							((TextView) findViewById(R.id.err_recipient_address)).setText(result.errorMessage);
						} else if (!TextUtils.isEmpty(result.errorMessage) && result.errorSource == GenerateTransactionResult.ERROR_SOURCE_UNKNOWN) {
//							new AlertDialog.Builder(MainActivity.this)
//									.setMessage(result.errorMessage)
//									.setPositiveButton(android.R.string.ok, null)
//									.show();
						}

						((TextView) findViewById(R.id.err_amount)).setText(
								result.errorSource == GenerateTransactionResult.ERROR_SOURCE_AMOUNT_FIELD ? result.errorMessage : "");
					}
				}

			}.execute();
		}













	}

	/**
	 * 交易的详细信息
	 * @param amountStr 转币金额
	 * @param outputs 使用的output集合
	 * @param feeStr 矿工费
	 * @param bitcoinCash 是不是比特币现金
	 * @param keyPair 密钥对
	 * @param outputAddress 收币地址
	 * @return
	 */
	@SuppressWarnings("IfCanBeSwitch")
	@NonNull
	private void getTxDescription(String amountStr, Transaction.Output[] outputs, String feeStr, boolean bitcoinCash, KeyPair keyPair, String outputAddress) {
		String changeStr;
		String descStr;
		if (outputs.length == 1) {
			changeStr = null;
			descStr = getString(bitcoinCash ? R.string.spend_bch_tx_description : R.string.spend_btc_tx_description,
					amountStr,
					keyPair.address,
					outputAddress,
					feeStr
			);




		} else if (outputs.length == 2) {
			changeStr = BTCUtils.formatValue(outputs[1].value);
			descStr = getString(bitcoinCash ? R.string.spend_bch_tx_with_change_description : R.string.spend_btc_tx_with_change_description,
					amountStr,//转币金额
					keyPair.address, //转币地址
					outputAddress, //收币地址
					feeStr, //矿工费
					changeStr //转币剩余的钱
			);
		} else {
			throw new RuntimeException();
		}

		tvTransactionInfo.setText(descStr);



//		String btcBch = bitcoinCash ? "BCH" : "BTC";
//		SpannableStringBuilder descBuilderBtc = new SpannableStringBuilder(descStr);
//
//		int spanBegin = keyPair.address == null ? -1 : descStr.indexOf(keyPair.address.addressString);
//		if (spanBegin >= 0) {//from
//			ForegroundColorSpan addressColorSpan = new ForegroundColorSpan(getColor(MainActivity.this, R.color.dark_orange));
//			descBuilderBtc.setSpan(addressColorSpan, spanBegin, spanBegin + keyPair.address.addressString.length(), SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE);
//		}
//		if (spanBegin >= 0) {
//			spanBegin = descStr.indexOf(keyPair.address.addressString, spanBegin + 1);
//			if (spanBegin >= 0) {//change
//				ForegroundColorSpan addressColorSpan = new ForegroundColorSpan(getColor(MainActivity.this, R.color.dark_orange));
//				descBuilderBtc.setSpan(addressColorSpan, spanBegin, spanBegin + keyPair.address.addressString.length(), SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE);
//			}
//		}
//		spanBegin = descStr.indexOf(outputAddress);
//		if (spanBegin >= 0) {//dest
//			ForegroundColorSpan addressColorSpan = new ForegroundColorSpan(getColor(MainActivity.this, R.color.dark_green));
//			descBuilderBtc.setSpan(addressColorSpan, spanBegin, spanBegin + outputAddress.length(), SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE);
//		}
//		final String nbspBtc = "\u00a0" + btcBch;
//		spanBegin = descStr.indexOf(amountStr + nbspBtc);
//		if (spanBegin >= 0) {
//			descBuilderBtc.setSpan(new StyleSpan(Typeface.BOLD), spanBegin, spanBegin + amountStr.length() + nbspBtc.length(), SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE);
//		}
//		spanBegin = descStr.indexOf(feeStr + nbspBtc, spanBegin);
//		if (spanBegin >= 0) {
//			descBuilderBtc.setSpan(new StyleSpan(Typeface.BOLD), spanBegin, spanBegin + feeStr.length() + nbspBtc.length(), SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE);
//		}
//		if (changeStr != null) {
//			spanBegin = descStr.indexOf(changeStr + nbspBtc, spanBegin);
//			if (spanBegin >= 0) {
//				descBuilderBtc.setSpan(new StyleSpan(Typeface.BOLD), spanBegin, spanBegin + changeStr.length() + nbspBtc.length(), SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE);
//			}
//		}
//		return descBuilderBtc;
	}





	///////////////////////////////////////////////////////////////////////////
	//摄像头扫码部分
	///////////////////////////////////////////////////////////////////////////

	@AfterPermissionGranted(REQUEST_CAMERA_PERM)
	public void cameraTask() {
		if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
			// Have permission, do the thing!
			onViewClick();
		} else {
			// Ask for one permission
			EasyPermissions.requestPermissions(this, "需要请求camera权限", REQUEST_CAMERA_PERM, Manifest.permission.CAMERA);
		}
	}

	/**
	 * 打开摄像头
	 */
	private void onViewClick() {
		if (CommonUtil.isCameraCanUse()) {
			Intent intent = new Intent(this, CaptureActivity.class);
			startActivityForResult(intent, REQUEST_CODE);
		} else {
			Toast.makeText(this, "打开摄像头权限", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		//扫描完的结果，之后就是恢复钱包
		//扫描结果回调
		if (resultCode == RESULT_OK) { //RESULT_OK = -1
			Bundle bundle = data.getExtras();
			String scanResult = bundle.getString("qr_scan_result");
			//将扫描出的信息显示出来
			//Toast.makeText(this, scanResult, Toast.LENGTH_SHORT).show();
			edToAddress.setText(scanResult);
		}
	}
}







































