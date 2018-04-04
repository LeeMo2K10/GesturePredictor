/*
 * Copyright (c) 2017 Villu Ruusmann
 *
 * This file is part of JPMML-SkLearn
 *
 * JPMML-SkLearn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-SkLearn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-SkLearn.  If not, see <http://www.gnu.org/licenses/>.
 */
package sklearn2pmml.preprocessing;

import java.util.List;

import sklearn.preprocessing.LabelBinarizer;

public class PMMLLabelBinarizer extends LabelBinarizer {

	public PMMLLabelBinarizer(String module, String name){
		super(module, name);
	}

	@Override
	protected List<?> prepareClasses(List<?> classes){

		if(classes.size() < 2){
			throw new IllegalArgumentException();
		}

		return classes;
	}

	@Override
	public Integer getPosLabel(){
		return 1;
	}

	@Override
	public Integer getNegLabel(){
		return 0;
	}
}