package com.gyx.bitcoinwalletdemo;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.gyx.bitcoinwalletdemo.bean.KeyStoreBean;
import com.gyx.bitcoinwalletdemo.eth.EthWalletUtil;
import com.gyx.bitcoinwalletdemo.util.KeyStoreUtils;
import com.gyx.bitcoinwalletdemo.zxing.activity.CaptureActivity;
import com.gyx.bitcoinwalletdemo.zxing.utils.CommonUtil;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class EthContractTransactionActivity extends BaseActivity implements View.OnClickListener {
	private TextView tvBalance;
	private Button btRefresh;
	private TextView tvSendAddress;
	private EditText edReceiveAddress;
	private Button btScanerAddress;
	private EditText edValue;
	private EditText edGasPrice;
	private EditText edGasLimit;
	private EditText edNonce;
	private Button btnCalculate;
	private TextView tvMiningPrice;
	private Button btnSend;
	private TextView tvMgs;
	private TextView tvContractAddress;
	private TextView tvContractSymbol;
	private Button btSendAll;
	//打开扫描界面请求码
	private static final int REQUEST_CODE = 0x01;
	//每一种代币有一个单独的合约地址
	final static String contract_address = "0x555df3a3e14268a28ba5f65d28db84569a2f7b4a";
	private String contract_balance;
	private TextView tvTokenName;
	private int tokenDecimals;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_eth_contract_transaction);
		initView();
	}

	private void initView() {
		tvBalance = (TextView) findViewById(R.id.tv_balance);
		tvTokenName = (TextView) findViewById(R.id.tv_token_name);
		tvContractAddress = (TextView) findViewById(R.id.tv_contract_address);
		btRefresh = (Button) findViewById(R.id.bt_refresh);
		//发送的地址
		tvSendAddress = (TextView) findViewById(R.id.tv_send_address);
		//接收的地址
		edReceiveAddress = (EditText) findViewById(R.id.ed_receive_address);
		btScanerAddress = (Button) findViewById(R.id.bt_scaner_address);
		edValue = (EditText) findViewById(R.id.ed_value);
		edGasPrice = (EditText) findViewById(R.id.ed_gas_price);
		edGasLimit = (EditText) findViewById(R.id.ed_gas_limit);
		edNonce = (EditText) findViewById(R.id.ed_nonce);
		btnCalculate = (Button) findViewById(R.id.btn_calculate);
		tvMiningPrice = (TextView) findViewById(R.id.tv_mining_price);
		btnSend = (Button) findViewById(R.id.btn_send);
		tvMgs = (TextView) findViewById(R.id.tv_mgs);
		//
		tvContractSymbol = (TextView) findViewById(R.id.tv_contract_symbol);
		btSendAll = (Button) findViewById(R.id.bt_send_all);
		btRefresh.setOnClickListener(this);
		btScanerAddress.setOnClickListener(this);
		btnCalculate.setOnClickListener(this);
		btnSend.setOnClickListener(this);
		btSendAll.setOnClickListener(this);
		//
		getNonceFromSendAddress();
		tvContractAddress.setText("合约地址：" + contract_address);
		//设置代币符号
		setSymbolText(contract_address);
		//查询代币名称
		setTokenName(contract_address);
		//获取代币的balance（这里需要查询精度，进行计算 tokenBalance = balance/10的精度次方）
		getTokenBalanceFromRPC();
	}

	private void setTokenName(final String contract_address) {
		Observable.create(new ObservableOnSubscribe<String>() {
			@Override
			public void subscribe(ObservableEmitter<String> e) throws Exception {
				String tokenName = getTokenName(contract_address);
				e.onNext(tokenName);
			}
		}).subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Observer<String>() {
					@Override
					public void onSubscribe(Disposable d) {
					}

					@Override
					public void onNext(String s) {
						tvTokenName.setText("代币名称:" + s);
					}

					@Override
					public void onError(Throwable e) {
					}

					@Override
					public void onComplete() {
					}
				});
	}

	private void setSymbolText(final String contract_address) {
		Observable.create(new ObservableOnSubscribe<String>() {
			@Override
			public void subscribe(ObservableEmitter<String> e) throws Exception {
				String tokenSymbol = getTokenSymbol(contract_address);
				e.onNext(tokenSymbol);
			}
		}).subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Consumer<String>() {
					@Override
					public void accept(String s) throws Exception {
						if (s != null) {
							tvContractSymbol.setText(s);
						}
					}
				});
	}

	/**
	 * 获取代币金额（这里需要查询精度，进行计算 tokenBalance = balance/10的精度次方）
	 */
	private void getTokenBalanceFromRPC() {
		List<KeyStoreBean> keyStoreBeans = EthWalletUtil.gekeystoreList();
//		for (KeyStoreBean bean : keyStoreBeans) {
//			fromAddress = bean.getAddress();
//			tvSendAddress.setText("ETH地址:0x" + fromAddress);
//		}
		final String address = keyStoreBeans.get(0).getAddress();
		Observable.create(new ObservableOnSubscribe<BigDecimal>() {
			@Override
			public void subscribe(ObservableEmitter<BigDecimal> e) throws Exception {
				BigInteger tokenBalance = getTokenBalance(address, contract_address);
				Log.e("代币数量（未处理数据）", tokenBalance + "");
				//获取代币的精度
				tokenDecimals = getTokenDecimals(contract_address);
				Log.e("代币精度", tokenDecimals + "");
				//计算真正的代币
				BigDecimal balance = new BigDecimal(tokenBalance);
				//tokenBalance = balance/10的精度次方
				BigDecimal afterCalculateTokenBalance = balance.divide(BigDecimal.TEN.pow(tokenDecimals));
				e.onNext(afterCalculateTokenBalance);
			}
		}).subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Consumer<BigDecimal>() {
					@Override
					public void accept(BigDecimal bigDecimal) throws Exception {
						if (bigDecimal != null) {
							contract_balance = bigDecimal.toPlainString();
							tvBalance.setText("代币Balance：" + contract_balance);
							Log.e("代币数量（处理后）", contract_balance);
						}
					}
				});
	}

	/**
	 * 查询代币符号
	 *
	 * @param contractAddress
	 * @return
	 */
	public String getTokenSymbol(String contractAddress) {
		String methodName = "symbol";
		String symbol = null;
		String fromAddr = "0x0000000000000000000000000000000000000000";
		List<Type> inputParameters = new ArrayList<>();
		List<TypeReference<?>> outputParameters = new ArrayList<>();
		TypeReference<Utf8String> typeReference = new TypeReference<Utf8String>() {
		};
		outputParameters.add(typeReference);
		Function function = new Function(methodName, inputParameters, outputParameters);
		String data = FunctionEncoder.encode(function);
		Transaction transaction = Transaction.createEthCallTransaction(fromAddr, contractAddress, data);
		EthCall ethCall;
		try {
			Web3j web3j = Web3JService.getInstance();
			ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).sendAsync().get();
			List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
			symbol = results.get(0).getValue().toString();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return symbol;
	}

	/**
	 * 查询代币名称
	 *
	 * @param contractAddress
	 * @return
	 */
	public String getTokenName(String contractAddress) {
		String methodName = "name";
		String name = null;
		String fromAddr = "0x0000000000000000000000000000000000000000";
		List<Type> inputParameters = new ArrayList<>();
		List<TypeReference<?>> outputParameters = new ArrayList<>();
		TypeReference<Utf8String> typeReference = new TypeReference<Utf8String>() {
		};
		outputParameters.add(typeReference);
		Function function = new Function(methodName, inputParameters, outputParameters);
		String data = FunctionEncoder.encode(function);
		Transaction transaction = Transaction.createEthCallTransaction(fromAddr, contractAddress, data);
		EthCall ethCall;
		try {
			Web3j web3j = Web3JService.getInstance();
			ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).sendAsync().get();
			List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
			name = results.get(0).getValue().toString();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return name;
	}

	/**
	 * 查询代币精度
	 *
	 * @param contractAddress
	 * @return
	 */
	public int getTokenDecimals(String contractAddress) {
		String methodName = "decimals";
		String fromAddr = "0x0000000000000000000000000000000000000000";
		int decimal = 0;
		List<Type> inputParameters = new ArrayList<>();
		List<TypeReference<?>> outputParameters = new ArrayList<>();
		TypeReference<Uint8> typeReference = new TypeReference<Uint8>() {
		};
		outputParameters.add(typeReference);
		Function function = new Function(methodName, inputParameters, outputParameters);
		String data = FunctionEncoder.encode(function);
		Transaction transaction = Transaction.createEthCallTransaction(fromAddr, contractAddress, data);
		EthCall ethCall;
		try {
			Web3j web3j = Web3JService.getInstance();
			ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).sendAsync().get();
			List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
			decimal = Integer.parseInt(results.get(0).getValue().toString());
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return decimal;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.bt_refresh:
				getTokenBalanceFromRPC();
				//获取普通交易的gas上限
				break;
			case R.id.bt_scaner_address:
				cameraTask();
				break;
			case R.id.btn_calculate:
				String gasPrice = edGasPrice.getText().toString().trim();
				String gasLimit = edGasLimit.getText().toString().trim();
				if (TextUtils.isEmpty(gasPrice) || TextUtils.isEmpty(gasLimit)) {
					Toast.makeText(this, "gasPrice和gasLimit不能为空", Toast.LENGTH_SHORT).show();
				} else {
					BigDecimal miningPrice = getMiningPrice(gasPrice, gasLimit);
					tvMiningPrice.setText("矿工费：" + miningPrice.toString() + "ETH");
				}
				break;

			//发送全部的时候，算出来的矿工费是，ETH，要自己判断ETH是否足够矿工费
			case R.id.bt_send_all:
				String gasPrice1 = edGasPrice.getText().toString().trim();
				String gasLimit1 = edGasLimit.getText().toString().trim();
				if (TextUtils.isEmpty(gasPrice1) || TextUtils.isEmpty(gasLimit1)) {
					Toast.makeText(this, "gasPrice和gasLimit不能为空", Toast.LENGTH_SHORT).show();
				} else {
					BigDecimal miningPrice = getMiningPrice(gasPrice1, gasLimit1);
					tvMiningPrice.setText("矿工费：" + miningPrice.toString() + "ETH");
					//设置转币的金额，是全部的代币
					edValue.setText(contract_balance);
				}
				break;
			//发送合约交易
			case R.id.btn_send:
				sendContractTransaction();
				break;
		}
	}

	/**
	 * 发送合约交易
	 */
	private void sendContractTransaction() {
		List<KeyStoreBean> keyStoreBeans = EthWalletUtil.gekeystoreList();
//		for (KeyStoreBean bean : keyStoreBeans) {
//			fromAddress = bean.getAddress();
//			tvSendAddress.setText("ETH地址:0x" + fromAddress);
//		}
		final String address = keyStoreBeans.get(0).getAddress();
		final String to = edReceiveAddress.getText().toString().trim();
		//
		String value = edValue.getText().toString().trim();
		BigDecimal bigDecimal = new BigDecimal(value);
		//用户所填写的数字*10的精度次方
		BigDecimal multiply = bigDecimal.multiply(BigDecimal.TEN.pow(tokenDecimals));
		final BigInteger amount = new BigInteger(multiply.toPlainString());
		//获取代币的精度

		//
		//final BigInteger bigValue = Convert.toWei(BigDecimal.valueOf(aDouble), Convert.Unit.ETHER).toBigInteger();
		//
		Observable.create(new ObservableOnSubscribe<String>() {
			@Override
			public void subscribe(ObservableEmitter<String> e) throws Exception {
				String txHash = sendTokenTransaction(address, to, contract_address, amount);
				if (txHash != null) {
					e.onNext(txHash);
				}
			}
		}).subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Observer<String>() {
					@Override
					public void onSubscribe(Disposable d) {
					}

					@Override
					public void onNext(String s) {
						Toast.makeText(EthContractTransactionActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
						tvMgs.setText(s);
					}

					@Override
					public void onError(Throwable e) {
						Toast.makeText(EthContractTransactionActivity.this, "发送失败", Toast.LENGTH_SHORT).show();
						tvMgs.setText(e.getMessage());

					}

					@Override
					public void onComplete() {
					}
				});
	}

	/**
	 * 代币转账
	 */
	public String sendTokenTransaction(String fromAddress, String toAddress, String contractAddress, BigInteger amount) throws IOException {
		String txHash = null;

			String methodName = "transfer";
			List<Type> inputParameters = new ArrayList<>();
			List<TypeReference<?>> outputParameters = new ArrayList<>();
			Address tAddress = new Address(toAddress);
			Uint256 value = new Uint256(amount);
			inputParameters.add(tAddress);
			inputParameters.add(value);
			TypeReference<Bool> typeReference = new TypeReference<Bool>() {
			};
			outputParameters.add(typeReference);
			Function function = new Function(methodName, inputParameters, outputParameters);
			String data = FunctionEncoder.encode(function);
			Web3j web3j = Web3JService.getInstance();
			//这段代码有问题
//			EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.PENDING).sendAsync().get();
//			BigInteger nonce = ethGetTransactionCount.getTransactionCount();

			//nonce
			EthGetTransactionCount count = Web3JService.getInstance().ethGetTransactionCount("0x" + fromAddress, DefaultBlockParameterName.LATEST).send();
			BigInteger nonce = Numeric.decodeQuantity(count.getResult());

			//gasPrice
			final String gasPrice = edGasPrice.getText().toString().trim();
			Double gasPriceDouble = Double.valueOf(gasPrice);
			final BigInteger bigGasPrice = Convert.toWei(BigDecimal.valueOf(gasPriceDouble), Convert.Unit.GWEI).toBigInteger();

			//gasLimit
			final String gasLimit = edGasLimit.getText().toString().trim();
			String hexValue = signedTransactionContractData(fromAddress, contractAddress, nonce.toString(), bigGasPrice.toString(), gasLimit, BigInteger.ZERO.toString(), data);
			EthSendTransaction send = web3j.ethSendRawTransaction(hexValue).send();
			txHash = send.getResult();
			return txHash;

	}

	/**
	 * 签名 合约交易
	 *
	 * @param from
	 * @param contractAddress
	 * @param nonce
	 * @param gasPrice
	 * @param gasLimit
	 * @param value
	 * @param data
	 * @return
	 * @throws FileNotFoundException
	 */
	public static String signedTransactionContractData(String from, String contractAddress, String nonce, String gasPrice, String gasLimit, String value, String data) throws FileNotFoundException {
		//发送正常交易
		RawTransaction rawTransaction = RawTransaction.createTransaction(
				new BigInteger(nonce),
				new BigInteger(gasPrice),
				new BigInteger(gasLimit),
				contractAddress,
				new BigInteger(value),
				data
		);
		//获取资格证书
		Credentials credentials = KeyStoreUtils.getCredentials(from);
		byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
		return Numeric.toHexString(signedMessage);
	}

	private void getNonceFromSendAddress() {
		List<KeyStoreBean> keyStoreBeans = EthWalletUtil.gekeystoreList();
//		for (KeyStoreBean bean : keyStoreBeans) {
//			fromAddress = bean.getAddress();
//			tvSendAddress.setText("ETH地址:0x" + fromAddress);
//		}
		final String address = keyStoreBeans.get(0).getAddress();
		tvSendAddress.setText("自己的地址:0x" + address);
		Observable.create(new ObservableOnSubscribe<EthGetTransactionCount>() {
			@Override
			public void subscribe(ObservableEmitter<EthGetTransactionCount> e) throws Exception {
				EthGetTransactionCount count = Web3JService.getInstance().ethGetTransactionCount("0x" + address, DefaultBlockParameterName.LATEST).send();
				if (count.getError() == null) {
					e.onNext(count);
					e.onComplete();
				} else {
					e.onError(new Throwable(count.getError().getMessage()));
				}
			}
		}).subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Observer<EthGetTransactionCount>() {
					@Override
					public void onSubscribe(Disposable d) {
					}

					@Override
					public void onNext(EthGetTransactionCount ethGetTransactionCount) {
						BigInteger bigInteger = Numeric.decodeQuantity(ethGetTransactionCount.getResult());
						edNonce.setText(bigInteger.toString());
					}

					@Override
					public void onError(Throwable e) {
					}

					@Override
					public void onComplete() {
					}
				});
	}

	/**
	 * 查询代币余额 (耗时操作)
	 */
	public static BigInteger getTokenBalance(String fromAddress, String contractAddress) {
		String methodName = "balanceOf";
		List<Type> inputParameters = new ArrayList<>();
		List<TypeReference<?>> outputParameters = new ArrayList<>();
		Address address = new Address(fromAddress);
		inputParameters.add(address);
		TypeReference<Uint256> typeReference = new TypeReference<Uint256>() {
		};
		outputParameters.add(typeReference);
		Function function = new Function(methodName, inputParameters, outputParameters);
		String data = FunctionEncoder.encode(function);
		Transaction transaction = Transaction.createEthCallTransaction(fromAddress, contractAddress, data);
		EthCall ethCall;
		BigInteger balanceValue = BigInteger.ZERO;
		try {
			Web3j web3j = Web3JService.getInstance();
			ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
			List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
			balanceValue = (BigInteger) results.get(0).getValue();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return balanceValue;
	}

	/**
	 * 计算矿工费
	 *
	 * @param gasPrice
	 * @param gasLimit
	 * @return
	 */
	private BigDecimal getMiningPrice(String gasPrice, String gasLimit) {
		Double gasPriceDouble = Double.valueOf(gasPrice);
		BigInteger bigGasPrice = Convert.toWei(BigDecimal.valueOf(gasPriceDouble), Convert.Unit.GWEI).toBigInteger();
		BigInteger gasLimitInteger = new BigInteger(gasLimit);
		BigInteger multiply = bigGasPrice.multiply(gasLimitInteger);
		Log.e("乘积", multiply.toString());
		//换算成ether单位
		BigDecimal bigDecimal2 = Convert.fromWei(new BigDecimal(multiply), Convert.Unit.ETHER);
		return bigDecimal2;
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
			edReceiveAddress.setText(scanResult);
		}
	}
}
