/*
Copyright 2008,2009 Complex Automata Simulation Technique (COAST) consortium

GNU Lesser General Public License

This file is part of MUSCLE (Multiscale Coupling Library and Environment).

    MUSCLE is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    MUSCLE is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with MUSCLE.  If not, see <http://www.gnu.org/licenses/>.
*/

package muscle.core.conduit.filter;

import muscle.core.Scale;
import muscle.core.wrapper.DataWrapper;
import muscle.core.DataTemplate;
import muscle.exception.MUSCLERuntimeException;
import utilities.array3d.Array3D_double;
import utilities.array2d.Array2D_double;
import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;


/**
maps 3d grid data to 2d data (calculates the average value for the third dimension)
@author Jan Hegewald
*/
public class ThreeD2TwoDFilterDouble implements muscle.core.conduit.filter.WrapperFilter<DataWrapper> {

	private WrapperFilter childFilter;	
	private DataTemplate inTemplate;
	
	//
	public ThreeD2TwoDFilterDouble(WrapperFilter newChildFilter) {
		
		childFilter = newChildFilter;
		DataTemplate outTemplate = childFilter.getInTemplate();
		if( outTemplate.getScale().getDimensions() != 2)
			throw new MUSCLERuntimeException("this filter must output 2D data");
		if( !outTemplate.getDataClass().equals(Array3D_double.class))
			throw new MUSCLERuntimeException("input must be a <"+Array3D_double.class+">");
		
		DecimalMeasure<Length>[] inDx = new DecimalMeasure[outTemplate.getScale().getDimensions()+1]; 
		System.arraycopy(outTemplate.getScale().getAllDx(), 0, inDx, 0, inDx.length);
		Scale inScale = new Scale(outTemplate.getScale().getDt(), inDx);
		
		inTemplate = new DataTemplate(Array2D_double.class, inScale);
	}
	
	
	//
	public DataTemplate getInTemplate() {
	
		return inTemplate;
	}


	// 
	public void put(DataWrapper input) {
				
		Array3D_double inData = (Array3D_double)input.getData();
		
		int width = inData.getX1Size();
		int height = inData.getX2Size();
		int depth = inData.getX3Size();

		// create array for our 2d data
		Array2D_double outData = new Array2D_double(width, height);

		// flatten 3d data
		double factor = 1.0/((double)depth);
		for(int ix = 0; ix < width; ix++) {
			for(int iy = 0; iy < height; iy++) {
				double val = 0.0;
				for(int iz = 0; iz < depth; iz++) {
					val += inData.get(ix, iy, iz)*factor;
				}
				
				outData.set(ix, iy, val);
			}
		}
		
		DataWrapper<Array2D_double> outWrapper = new DataWrapper<Array2D_double>(outData, input.getSITime());
		childFilter.put(outWrapper);
	}

}
