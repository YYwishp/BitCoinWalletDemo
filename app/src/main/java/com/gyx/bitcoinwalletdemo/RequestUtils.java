package com.gyx.bitcoinwalletdemo;

import android.content.Context;
import android.text.TextUtils;

import com.gyx.bitcoinwalletdemo.bean.AddressBalanceBean;
import com.gyx.bitcoinwalletdemo.callback.DialogCallback;
import com.gyx.bitcoinwalletdemo.callback.OnDownLoadListener;
import com.lzy.okgo.OkGo;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.Response;

/**
 * Created by gyx on 2017/10/11.
 */
public class RequestUtils {
	private static RequestUtils instance;


	private RequestUtils() {
	}

	/**
	 * 单一实例
	 */
	public static RequestUtils getInstance() {
		if (instance == null) {
			instance = new RequestUtils();
		}
		return instance;
	}

	private void setHost() {
//		String host = (String) SPUtils.get("host", "");
//		if (TextUtils.isEmpty(host)) {
//		} else {
//			HOST = host;
//		}
	}

	/**
	 * 历史记录
	 *
	 * @param active             就是地址
	 * @param onDownDataListener
	 */
	public void getAddressBalance(String active, final OnDownLoadListener<AddressBalanceBean> onDownDataListener) {
		setHost();
		//请求网络
		OkGo.get(Urls.BALANCE)//
				.tag(this)//
				.params("active", active)//
				.params("cors", true)//
				.execute(new DialogCallback<AddressBalanceBean>() {
					@Override
					public void onSuccess(AddressBalanceBean beanArrayList, Call call, Response response) {
						onDownDataListener.onSuccess(beanArrayList);
					}
//

					@Override
					public AddressBalanceBean convertSuccess(Response response) throws Exception {
						String string = response.body().string();
						//
//						AddressBalanceBean bean = Convert.fromJson(string, AddressBalanceBean.class);
						return null;
					}

					@Override
					public void onError(Call call, Response response, Exception e) {
						onDownDataListener.onFailed(e);
						super.onError(call, response, e);
					}
				});
	}

	/**
	 * 创建钱包
	 *
	 * @param onDownDataListener
	 */
	public void getUnspent( String active, final OnDownLoadListener<AddressBalanceBean> onDownDataListener) {
		setHost();
		//请求网络
		OkGo.get(Urls.UNSPENT)//
				.tag(this)//
				.params("active", active)//
				.params("cors", true)//
				.execute(new DialogCallback<AddressBalanceBean>() {
					@Override
					public void onSuccess(AddressBalanceBean bean, Call call, Response response) {
						onDownDataListener.onSuccess(bean);

					}

					@Override
					public AddressBalanceBean convertSuccess(Response response) throws Exception {


						return null;
					}

					@Override
					public void onError(Call call, Response response, Exception e) {
						onDownDataListener.onFailed(e);
						super.onError(call, response, e);
					}
				});
	}
}



























