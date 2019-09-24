/*
 * Copyright (C) 2019 denkbares GmbH, Germany
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package com.denkbares.strings;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.denkbares.utils.Pair;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * The rules are derived fom <a href="https://github.com/doctrine/inflector">inflector</a> project, licensed as MIT
 * license.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 31.08.2017
 */
public class Inflector {

	@SuppressWarnings("unchecked")
	private static final Pair<Pattern, String>[] pluralRules = new Pair[] {
			new Pair<>(Pattern.compile("(s)tatus$", CASE_INSENSITIVE), "$1tatuses"),
			new Pair<>(Pattern.compile("(quiz)$", CASE_INSENSITIVE), "$1zes"),
			new Pair<>(Pattern.compile("^(ox)$", CASE_INSENSITIVE), "$1en"),
			new Pair<>(Pattern.compile("([m|l])ouse$", CASE_INSENSITIVE), "$1ice"),
			new Pair<>(Pattern.compile("(matr|vert|ind)(ix|ex)$", CASE_INSENSITIVE), "$1ices"),
			new Pair<>(Pattern.compile("(x|ch|ss|sh)$", CASE_INSENSITIVE), "$1es"),
			new Pair<>(Pattern.compile("([^aeiouy]|qu)y$", CASE_INSENSITIVE), "$1ies"),
			new Pair<>(Pattern.compile("(hive|gulf)$", CASE_INSENSITIVE), "$1s"),
			new Pair<>(Pattern.compile("(?:([^f])fe|([lr])f)$", CASE_INSENSITIVE), "$1$2ves"),
			new Pair<>(Pattern.compile("sis$", CASE_INSENSITIVE), "ses"),
			new Pair<>(Pattern.compile("([ti])um$", CASE_INSENSITIVE), "$1a"),
			new Pair<>(Pattern.compile("(c)riterion$", CASE_INSENSITIVE), "$1riteria"),
			new Pair<>(Pattern.compile("(p)erson$", CASE_INSENSITIVE), "$1eople"),
			new Pair<>(Pattern.compile("(m)an$", CASE_INSENSITIVE), "$1en"),
			new Pair<>(Pattern.compile("(c)hild$", CASE_INSENSITIVE), "$1hildren"),
			new Pair<>(Pattern.compile("(f)oot$", CASE_INSENSITIVE), "$1eet"),
			new Pair<>(Pattern.compile("(buffal|her|potat|tomat|volcan)o$", CASE_INSENSITIVE), "$1oes"),
			new Pair<>(Pattern.compile("(alumn|bacill|cact|foc|fung|nucle|radi|stimul|syllab|termin|vir)us$", CASE_INSENSITIVE), "$1i"),
			new Pair<>(Pattern.compile("us$", CASE_INSENSITIVE), "uses"),
			new Pair<>(Pattern.compile("(alias)$", CASE_INSENSITIVE), "$1es"),
			new Pair<>(Pattern.compile("(analys|ax|cris|test|thes)is$", CASE_INSENSITIVE), "$1es"),
			new Pair<>(Pattern.compile("s$", CASE_INSENSITIVE), "s"),
			new Pair<>(Pattern.compile("^$", CASE_INSENSITIVE), ""),
			new Pair<>(Pattern.compile("$", CASE_INSENSITIVE), "s"),
	};

	private static final Pattern[] pluralUninflected = new Pattern[] {
			Pattern.compile(".*[nrlm]ese$", CASE_INSENSITIVE),
			Pattern.compile(".*deer$", CASE_INSENSITIVE),
			Pattern.compile(".*fish$", CASE_INSENSITIVE),
			Pattern.compile(".*measles$", CASE_INSENSITIVE),
			Pattern.compile(".*ois$", CASE_INSENSITIVE),
			Pattern.compile(".*pox$", CASE_INSENSITIVE),
			Pattern.compile(".*sheep$", CASE_INSENSITIVE),
			Pattern.compile("people$", CASE_INSENSITIVE),
			Pattern.compile("cookie$", CASE_INSENSITIVE),
			Pattern.compile("police$", CASE_INSENSITIVE),
	};

	private static final Map<String, String> pluralIrregular = new HashMap<>();

	static {
		pluralIrregular.put("atlas", "atlases");
		pluralIrregular.put("axe", "axes");
		pluralIrregular.put("beef", "beefs");
		pluralIrregular.put("brother", "brothers");
		pluralIrregular.put("cafe", "cafes");
		pluralIrregular.put("chateau", "chateaux");
		pluralIrregular.put("niveau", "niveaux");
		pluralIrregular.put("child", "children");
		pluralIrregular.put("cookie", "cookies");
		pluralIrregular.put("corpus", "corpuses");
		pluralIrregular.put("cow", "cows");
		pluralIrregular.put("criterion", "criteria");
		pluralIrregular.put("curriculum", "curricula");
		pluralIrregular.put("demo", "demos");
		pluralIrregular.put("domino", "dominoes");
		pluralIrregular.put("echo", "echoes");
		pluralIrregular.put("foot", "feet");
		pluralIrregular.put("fungus", "fungi");
		pluralIrregular.put("ganglion", "ganglions");
		pluralIrregular.put("genie", "genies");
		pluralIrregular.put("genus", "genera");
		pluralIrregular.put("graffito", "graffiti");
		pluralIrregular.put("hippopotamus", "hippopotami");
		pluralIrregular.put("hoof", "hoofs");
		pluralIrregular.put("human", "humans");
		pluralIrregular.put("iris", "irises");
		pluralIrregular.put("larva", "larvae");
		pluralIrregular.put("leaf", "leaves");
		pluralIrregular.put("loaf", "loaves");
		pluralIrregular.put("man", "men");
		pluralIrregular.put("medium", "media");
		pluralIrregular.put("memorandum", "memoranda");
		pluralIrregular.put("money", "monies");
		pluralIrregular.put("mongoose", "mongooses");
		pluralIrregular.put("motto", "mottoes");
		pluralIrregular.put("move", "moves");
		pluralIrregular.put("mythos", "mythoi");
		pluralIrregular.put("niche", "niches");
		pluralIrregular.put("nucleus", "nuclei");
		pluralIrregular.put("numen", "numina");
		pluralIrregular.put("occiput", "occiputs");
		pluralIrregular.put("octopus", "octopuses");
		pluralIrregular.put("opus", "opuses");
		pluralIrregular.put("ox", "oxen");
		pluralIrregular.put("passerby", "passersby");
		pluralIrregular.put("penis", "penises");
		pluralIrregular.put("person", "people");
		pluralIrregular.put("plateau", "plateaux");
		pluralIrregular.put("runner-up", "runners-up");
		pluralIrregular.put("sex", "sexes");
		pluralIrregular.put("soliloquy", "soliloquies");
		pluralIrregular.put("son-in-law", "sons-in-law");
		pluralIrregular.put("syllabus", "syllabi");
		pluralIrregular.put("testis", "testes");
		pluralIrregular.put("thief", "thieves");
		pluralIrregular.put("tooth", "teeth");
		pluralIrregular.put("tornado", "tornadoes");
		pluralIrregular.put("trilby", "trilbys");
		pluralIrregular.put("turf", "turfs");
		pluralIrregular.put("volcano", "volcanoes");
	}

	public static String pluralOf(String word) {
		String irregular = pluralIrregular.get(word.toLowerCase());
		if (irregular != null) {
			if (Character.isLowerCase(word.charAt(0))) return irregular;
			if (Character.isUpperCase(word.charAt(1))) return irregular.toUpperCase();
			return irregular.substring(0, 1).toUpperCase() + irregular.substring(1);
		}

		for (Pattern pattern : pluralUninflected) {
			if (pattern.matcher(word).matches()) {
				return word;
			}
		}

		for (Pair<Pattern, String> rule : pluralRules) {
			Matcher matcher = rule.getA().matcher(word);
			if (matcher.find()) {
				return matcher.replaceFirst(rule.getB());
			}
		}
		return word;
	}
}
