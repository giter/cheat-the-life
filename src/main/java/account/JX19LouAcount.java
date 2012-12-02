package account;

import giter.HttpClient;

import java.io.IOException;
import java.net.UnknownHostException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;

public class JX19LouAcount {

	static HttpClient util = new HttpClient().connect(3000).read(30000)
			.persistCookies(false).cookie("_Z3nY0d4C_", "37XgPK9h");

	public static void main(String[] args) throws UnknownHostException {

		Mongo m = new Mongo("127.0.0.1");
		DBCollection collection = m.getDB("jx19lou").getCollection("account");

		for (int i = 1; i < 50000; i++) {

			try {

				Document doc = Jsoup
						.parse(util
								.GET(String
										.format("http://jiaxing.19lou.com/user/profile-330%05d-1.html",
												i)).getValue());
				Element ele = doc.select(".ta-home a").first();

				if (ele != null) {
					collection.save(BasicDBObjectBuilder
							.start("_id", ele.text())
							.add("uid", String.format("330%05d", i)).get());
				}

			} catch (IOException e) {
			}

			if (i % 100 == 0) {
				System.out.println(i);
			}
		}

		m.close();
	}

}
