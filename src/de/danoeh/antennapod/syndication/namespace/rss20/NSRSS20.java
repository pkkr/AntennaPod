package de.danoeh.antennapod.syndication.namespace.rss20;

import java.util.ArrayList;
import java.util.Date;

import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedImage;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.syndication.handler.HandlerState;
import de.danoeh.antennapod.syndication.handler.SyndHandler;
import de.danoeh.antennapod.syndication.namespace.Namespace;
import de.danoeh.antennapod.syndication.namespace.SyndElement;
import de.danoeh.antennapod.syndication.util.SyndDateUtils;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX-Parser for reading RSS-Feeds
 * 
 * @author daniel
 * 
 */
public class NSRSS20 extends Namespace {
	public static final String NSTAG = "rss";
	public static final String NSURI = "";

	public final static String CHANNEL = "channel";
	public final static String ITEM = "item";
	public final static String TITLE = "title";
	public final static String LINK = "link";
	public final static String DESCR = "description";
	public final static String PUBDATE = "pubDate";
	public final static String ENCLOSURE = "enclosure";
	public final static String IMAGE = "image";
	public final static String URL = "url";
	public final static String LANGUAGE = "language";

	public final static String ENC_URL = "url";
	public final static String ENC_LEN = "length";
	public final static String ENC_TYPE = "type";

	public final static String VALID_MIMETYPE = "audio/.*" + "|" + "video/.*";

	@Override
	public SyndElement handleElementStart(String localName, HandlerState state,
			Attributes attributes) {
		if (localName.equals(ITEM)) {
			state.setCurrentItem(new FeedItem());
			state.getFeed().getItems().add(state.getCurrentItem());
			state.getCurrentItem().setFeed(state.getFeed());

		} else if (localName.equals(ENCLOSURE)) {
			String type = attributes.getValue(ENC_TYPE);
			if (state.getCurrentItem().getMedia() == null
					&& (type.matches(VALID_MIMETYPE))) {
				state.getCurrentItem().setMedia(
						new FeedMedia(state.getCurrentItem(), attributes
								.getValue(ENC_URL), Long.parseLong(attributes
								.getValue(ENC_LEN)), attributes
								.getValue(ENC_TYPE)));
			}

		} else if (localName.equals(IMAGE)) {
			state.getFeed().setImage(new FeedImage());
		}
		return new SyndElement(localName, this);
	}

	@Override
	public void handleElementEnd(String localName, HandlerState state) {
		if (localName.equals(ITEM)) {
			state.setCurrentItem(null);
		} else if (state.getTagstack().size() >= 2
				&& state.getContentBuf() != null) {
			String content = state.getContentBuf().toString();
			SyndElement topElement = state.getTagstack().peek();
			String top = topElement.getName();
			SyndElement secondElement = state.getSecondTag();
			String second = secondElement.getName();
			if (top.equals(TITLE)) {
				if (second.equals(ITEM)) {
					state.getCurrentItem().setTitle(content);
				} else if (second.equals(CHANNEL)) {
					state.getFeed().setTitle(content);
				} else if (second.equals(IMAGE)) {
					state.getFeed().getImage().setTitle(IMAGE);
				}
			} else if (top.equals(LINK)) {
				if (second.equals(CHANNEL)) {
					state.getFeed().setLink(content);
				} else if (second.equals(ITEM)) {
					state.getCurrentItem().setLink(content);
				}
			} else if (top.equals(PUBDATE) && second.equals(ITEM)) {
				state.getCurrentItem().setPubDate(
						SyndDateUtils.parseRFC822Date(content));
			} else if (top.equals(URL) && second.equals(IMAGE)) {
				state.getFeed().getImage().setDownload_url(content);
			} else if (localName.equals(DESCR)) {
				if (second.equals(CHANNEL)) {
					state.getFeed().setDescription(content);
				} else if (second.equals(ITEM)){
					state.getCurrentItem().setDescription(content);
				}

			} else if (localName.equals(LANGUAGE)) {
				state.getFeed().setLanguage(content.toLowerCase());
			}
		}
	}

}
