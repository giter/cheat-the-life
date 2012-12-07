package util;

import giter.HttpClient;
import giter.Proxier;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketAddress;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultProxier implements Proxier {

	final static HttpClient						hc			= new HttpClient().connect(5000);
	private final static String				URL;

	static {
		Properties properties = new Properties();
		try {
			properties.load(DefaultProxier.class.getResourceAsStream("/resources.properties"));
			URL = properties.get("proxier").toString();
		} catch (IOException e) {
			throw new RuntimeException("No properties specified!", e);
		}
	}

	private static final Logger				logger	= LoggerFactory.getLogger(DefaultProxier.class);

	private final static Queue<Proxy>	proxies	= new ConcurrentLinkedQueue<Proxy>();

	public static void fillPool() {

		if (logger.isDebugEnabled()) {
			logger.debug("Loading proxy list: " + URL);
		}

		for (int i = 0; i < 0x7fffffff; i++) {
			try {

				for (String s : hc.GET(URL).getValue().split("\r?\n")) {
					String host = s.substring(0, s.indexOf(':'));
					int port = NumberUtils.toInt(s.substring(s.indexOf(':') + 1));
					SocketAddress sa = new InetSocketAddress(host, port);
					Proxy p = new Proxy(Type.HTTP, sa);
					proxies.add(p);
				}

				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Loaded %d proxies", proxies.size()));
				}

				try {
					Thread.sleep(new Random().nextInt(5) * 1000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}

				return;
			} catch (IOException e) {
				logger.warn("Cannot access proxy at " + i + " times , try again!");
			}

			try {
				Thread.sleep(new Random().nextInt(5) * 1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

	}

	@Override
	public Proxy get() {

		while (proxies.isEmpty()) {
			synchronized (proxies) {
				if (proxies.isEmpty()) {
					fillPool();
				}
			}
		}

		return proxies.remove();

	}
}
