package com.gyx.bitcoinwalletdemo;

import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

/**
 * Created by gyx on 2018/4/4.
 */
public final  class Constants {


	public static final boolean TEST = true;

	/** Network this wallet is on (e.g. testnet or mainnet). */
	public static final NetworkParameters NETWORK_PARAMETERS = TEST ? TestNet3Params.get() : MainNetParams.get();

	/** Bitcoinj global context. */
	public static final Context CONTEXT = new Context(NETWORK_PARAMETERS);



}
