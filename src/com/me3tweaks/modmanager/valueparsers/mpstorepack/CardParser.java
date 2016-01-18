package com.me3tweaks.modmanager.valueparsers.mpstorepack;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class CardParser {
	public static void main(String args[]) throws Exception {
		HashMap<String, StorePack> packnameMap = new HashMap<String, StorePack>();
		XPath xpath = XPathFactory.newInstance().newXPath();
		String packlistExpression = "/CoalesceAsset/Sections/Section[@name='sfxgamempcontent.sfxgawreinforcementmanager']/Property[@name='packlist']/Value[@type='3']";
		String poolnameExpression = "/CoalesceAsset/Sections/Section/Property[@name='poolname']";
		String poolcontentsExpression = "../Property[@name='cardlist']/Value"; //relative to poolname node
		String singlepoolcontentsExpression = "../Property[@name='cardlist' and @type=3]";
		InputSource inputSource = new InputSource("file:///" + System.getProperty("user.dir") + "\\carddata\\biogame_liveini.xml");

		//PARSE - POOLS
		NodeList poolNodes = (NodeList) xpath.evaluate(poolnameExpression, inputSource, XPathConstants.NODESET);
		System.out.println("Number of nodes: " + poolNodes.getLength());
		if (poolNodes != null && poolNodes.getLength() > 0) {
			for (int i = 0; i < poolNodes.getLength(); i++) {
				if (poolNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
					Element el = (Element) poolNodes.item(i);
					System.out.println("POOL: " + el.getTextContent());
					NodeList contentNodes = (NodeList) xpath.evaluate(poolcontentsExpression, el, XPathConstants.NODESET);
					//GET CONTENTS OF POOL
					if (contentNodes != null && contentNodes.getLength() > 0) {
						for (int x = 0; x < contentNodes.getLength(); x++) {
							if (contentNodes.item(x).getNodeType() == Node.ELEMENT_NODE) {
								Element contentNode = (Element) contentNodes.item(x);
								System.out.println("---ITEM: " + contentNode.getTextContent());
							}
						}
					} else {
						//try a 1-item array search.
						contentNodes = (NodeList) xpath.evaluate(singlepoolcontentsExpression, el, XPathConstants.NODESET);
						//GET CONTENTS OF POOL
						if (contentNodes != null && contentNodes.getLength() > 0) {
							for (int x = 0; x < contentNodes.getLength(); x++) {
								if (contentNodes.item(x).getNodeType() == Node.ELEMENT_NODE) {
									Element contentNode = (Element) contentNodes.item(x);
									System.out.println("---ITEM: " + Card.getHumanName(contentNode.getTextContent()));
								}
							}
						}
					}
				}
			}
		}

		if (true) {
			System.exit(0);
		}
		//PARSE - SLOTS
		NodeList nodes = (NodeList) xpath.evaluate(packlistExpression, inputSource, XPathConstants.NODESET);

		System.out.println("Number of nodes: " + nodes.getLength());

		if (nodes != null && nodes.getLength() > 0) {
			for (int i = 0; i < nodes.getLength(); i++) {
				if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
					Element el = (Element) nodes.item(i);
					String packData = el.getTextContent();
					if (packData.equals("null")) {
						continue;
					}

					//System.out.println("parsing: "+packData);
					PackSlot packslot = new PackSlot(packData);
					StorePack containingPack = packnameMap.get(packslot.getPackname());
					if (containingPack != null) {
						containingPack.addCardSlot(packslot);
					} else {
						StorePack newPack = new StorePack();
						newPack.addCardSlot(packslot);
						packnameMap.put(packslot.getPackname(), newPack);
					}
				}
			}

			for (Entry<String, StorePack> entry : packnameMap.entrySet()) {
				String packname = entry.getKey();
				StorePack pack = entry.getValue();
				System.out.println(packname + ": " + pack.getContentsString());
			}
		}
	}
}
