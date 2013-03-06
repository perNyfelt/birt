/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.item.crosstab.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.birt.core.data.ExpressionUtil;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.IBinding;
import org.eclipse.birt.report.data.adapter.api.CubeQueryUtil;
import org.eclipse.birt.report.data.adapter.api.DataRequestSession;
import org.eclipse.birt.report.data.adapter.api.DataSessionContext;
import org.eclipse.birt.report.data.adapter.api.IDimensionLevel;
import org.eclipse.birt.report.data.adapter.api.IModelAdapter;
import org.eclipse.birt.report.data.adapter.api.IModelAdapter.ExpressionLocation;
import org.eclipse.birt.report.item.crosstab.core.CrosstabException;
import org.eclipse.birt.report.item.crosstab.core.ICrosstabConstants;
import org.eclipse.birt.report.item.crosstab.core.ICrosstabReportItemConstants;
import org.eclipse.birt.report.item.crosstab.core.de.AggregationCellHandle;
import org.eclipse.birt.report.item.crosstab.core.de.CrosstabReportItemHandle;
import org.eclipse.birt.report.item.crosstab.core.de.DimensionViewHandle;
import org.eclipse.birt.report.item.crosstab.core.de.LevelViewHandle;
import org.eclipse.birt.report.item.crosstab.core.de.MeasureViewHandle;
import org.eclipse.birt.report.item.crosstab.core.de.internal.CrosstabModelUtil;
import org.eclipse.birt.report.item.crosstab.core.i18n.Messages;
import org.eclipse.birt.report.model.api.ComputedColumnHandle;
import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.LabelHandle;
import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.birt.report.model.api.PropertyHandle;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.api.extension.ExtendedElementException;
import org.eclipse.birt.report.model.api.extension.IReportItem;
import org.eclipse.birt.report.model.api.metadata.PropertyValueException;
import org.eclipse.birt.report.model.api.olap.CubeHandle;
import org.eclipse.birt.report.model.api.olap.DimensionHandle;
import org.eclipse.birt.report.model.api.olap.LevelHandle;
import org.eclipse.birt.report.model.api.olap.MeasureGroupHandle;
import org.eclipse.birt.report.model.api.olap.MeasureHandle;
import org.eclipse.birt.report.model.api.util.CubeUtil;

/**
 * Utility clas for crosstab.
 */

public final class CrosstabUtil implements ICrosstabConstants
{

	private CrosstabUtil( )
	{
	}

	public static void setCrosstabUpdateListener(
			ICrosstabUpdateListener listener )
	{
		CrosstabModelUtil.setCrosstabModelListener( listener );
	}

	public static ICrosstabUpdateListener getCrosstabUpdateListener( )
	{
		Object listener = CrosstabModelUtil.getCrosstabModelListener( );

		if ( listener instanceof ICrosstabUpdateListener )
		{
			return (ICrosstabUpdateListener) listener;
		}

		return null;
	}

	public static int getOppositeAxisType( int axisType )
	{
		switch ( axisType )
		{
			case COLUMN_AXIS_TYPE :
				return ROW_AXIS_TYPE;
			case ROW_AXIS_TYPE :
				return COLUMN_AXIS_TYPE;
			default :
				return NO_AXIS_TYPE;
		}
	}

	/**
	 * 
	 * @param element
	 * @return report item if found, otherwise null
	 */
	public static IReportItem getReportItem( DesignElementHandle element )
	{
		if ( !( element instanceof ExtendedItemHandle ) )
			return null;
		ExtendedItemHandle extendedItem = (ExtendedItemHandle) element;
		try
		{
			return extendedItem.getReportItem( );
		}
		catch ( ExtendedElementException e )
		{
			return null;
		}
	}

	/**
	 * 
	 * @param element
	 * @param extensionName
	 * @return report item if found, otherwise null
	 */
	public static IReportItem getReportItem( DesignElementHandle element,
			String extensionName )
	{
		if ( !( element instanceof ExtendedItemHandle ) )
			return null;
		if ( extensionName == null )
			throw new IllegalArgumentException( "extension name can not be null" ); //$NON-NLS-1$
		ExtendedItemHandle extendedItem = (ExtendedItemHandle) element;
		if ( extensionName.equals( extendedItem.getExtensionName( ) ) )
		{
			try
			{
				return extendedItem.getReportItem( );
			}
			catch ( ExtendedElementException e )
			{
				return null;
			}
		}
		return null;
	}

	/**
	 * Check the containment logic for dimension from outside of crosstab
	 * 
	 * @param crosstab
	 * @param axisType
	 * @param dimension
	 * @return
	 */
	public static boolean canContain( CrosstabReportItemHandle crosstab,
			DimensionHandle dimension )
	{
		if ( crosstab != null
				&& crosstab.getModelHandle( ).getExtends( ) != null )
			return false;

		if ( crosstab != null && dimension != null )
		{
			CubeHandle currentCube = crosstab.getCube( );

			if ( currentCube == null )
			{
				return true;
			}

			// check containment consistence
			if ( dimension.getElement( )
					.isContentOf( currentCube.getElement( ) ) )
			{
				for ( int i = 0; i < crosstab.getDimensionCount( ROW_AXIS_TYPE ); i++ )
				{
					DimensionViewHandle dv = crosstab.getDimension( ROW_AXIS_TYPE,
							i );

					if ( dv.getCubeDimension( ) == dimension )
					{
						return false;
					}
				}

				for ( int i = 0; i < crosstab.getDimensionCount( COLUMN_AXIS_TYPE ); i++ )
				{
					DimensionViewHandle dv = crosstab.getDimension( COLUMN_AXIS_TYPE,
							i );

					if ( dv.getCubeDimension( ) == dimension )
					{
						return false;
					}
				}

				return true;
			}

		}

		return false;
	}

	/**
	 * Check the containment logic for measure from outside of crosstab
	 * 
	 * @param crosstab
	 * @param measure
	 * @return
	 */
	public static boolean canContain( CrosstabReportItemHandle crosstab,
			MeasureHandle measure )
	{
		if ( crosstab != null
				&& crosstab.getModelHandle( ).getExtends( ) != null )
			return false;

		if ( crosstab != null && measure != null )
		{
			CubeHandle currentCube = crosstab.getCube( );

			if ( currentCube == null )
			{
				return true;
			}

			// check containment consistence
			if ( measure.getElement( ).isContentOf( currentCube.getElement( ) ) )
			{
				for ( int i = 0; i < crosstab.getMeasureCount( ); i++ )
				{
					MeasureViewHandle mv = crosstab.getMeasure( i );

					if ( mv.getCubeMeasure( ) == measure )
					{
						return false;
					}
				}

				return true;
			}

		}

		return false;
	}

	public static boolean canContain( CrosstabReportItemHandle crosstab,
			MeasureGroupHandle obj )
	{
		return crosstab.getModelHandle( ).getExtends( ) == null;
	}

	/**
	 * Checks if the add/remove aggregation operation should be perform on all
	 * measures on given axis type
	 */
	public static boolean isAggregationAffectAllMeasures(
			CrosstabReportItemHandle crosstabItem, int axisType )
	{
		String measureDirection = crosstabItem.getMeasureDirection( );

		if ( ( MEASURE_DIRECTION_HORIZONTAL.equals( measureDirection ) && ( axisType == ROW_AXIS_TYPE ) )
				|| ( MEASURE_DIRECTION_VERTICAL.equals( measureDirection ) && ( axisType == COLUMN_AXIS_TYPE ) ) )
		{
			return true;
		}

		return false;
	}

	public static String getDefaultMeasureAggregationFunction(
			MeasureViewHandle mv )
	{
		return CrosstabModelUtil.getDefaultMeasureAggregationFunction( mv );
	}

	public static void addDataItem( CrosstabReportItemHandle crosstab,
			AggregationCellHandle cell, MeasureViewHandle measureView,
			String function, String rowDimension, String rowLevel,
			String colDimension, String colLevel ) throws SemanticException
	{
		CrosstabModelUtil.addDataItem( crosstab,
				cell,
				measureView,
				function,
				rowDimension,
				rowLevel,
				colDimension,
				colLevel );
	}

	public static List<IDimensionLevel> getReferencedLevels(
			LevelViewHandle level, String bindingExpr )
	{
		LevelHandle levelHandle = level.getCubeLevel( );
		if ( level.getCubeLevel( ) == null )
		{
			return Collections.EMPTY_LIST;
		}

		// get targetLevel
		DesignElementHandle hierarchyHandle = levelHandle.getContainer( );
		DesignElementHandle dimensionHandle = hierarchyHandle == null ? null
				: hierarchyHandle.getContainer( );
		if ( dimensionHandle == null )
		{
			return Collections.EMPTY_LIST;
		}

		String targetLevel = ExpressionUtil.createJSDimensionExpression( dimensionHandle.getName( ),
				levelHandle.getName( ) );

		CrosstabReportItemHandle crosstab = level.getCrosstab( );

		try
		{
			List<IBinding> bindings = getQueryBindings( crosstab );
			List<String> rowExpList = getLevelExpressionList( crosstab,
					ICrosstabConstants.ROW_AXIS_TYPE );
			List<String> colExpList = getLevelExpressionList( crosstab,
					ICrosstabConstants.COLUMN_AXIS_TYPE );

			return CubeQueryUtil.getReferencedLevels( targetLevel,
					bindingExpr,
					bindings,
					rowExpList,
					colExpList );
		}
		catch ( Exception e )
		{
			e.printStackTrace( );
		}

		return Collections.EMPTY_LIST;
	}

	private static List<IBinding> getQueryBindings(
			CrosstabReportItemHandle crosstabItem ) throws BirtException
	{
		List<String> rowLevelNameList = new ArrayList<String>( );
		List<String> columnLevelNameList = new ArrayList<String>( );

		// add row edge
		if ( crosstabItem.getDimensionCount( ICrosstabConstants.ROW_AXIS_TYPE ) > 0 )
		{
			// TODO check visibility?

			for ( int i = 0; i < crosstabItem.getDimensionCount( ICrosstabConstants.ROW_AXIS_TYPE ); i++ )
			{
				DimensionViewHandle dv = crosstabItem.getDimension( ICrosstabConstants.ROW_AXIS_TYPE,
						i );

				if ( dv.getCubeDimension( ) == null )
				{
					throw new CrosstabException( dv.getModelHandle( )
							.getElement( ),
							Messages.getString( "CrosstabQueryHelper.error.invalid.dimension.row", //$NON-NLS-1$
									dv.getCubeDimensionName( ) ) );
				}

				for ( int j = 0; j < dv.getLevelCount( ); j++ )
				{
					LevelViewHandle lv = dv.getLevel( j );

					if ( lv.getCubeLevel( ) == null )
					{
						throw new CrosstabException( lv.getModelHandle( )
								.getElement( ),
								Messages.getString( "CrosstabQueryHelper.error.invalid.level.row", //$NON-NLS-1$
										lv.getCubeLevelName( ) ) );
					}

					rowLevelNameList.add( lv.getCubeLevel( ).getFullName( ) );
				}
			}

		}

		// add column edge
		if ( crosstabItem.getDimensionCount( ICrosstabConstants.COLUMN_AXIS_TYPE ) > 0 )
		{
			// TODO check visibility?

			for ( int i = 0; i < crosstabItem.getDimensionCount( ICrosstabConstants.COLUMN_AXIS_TYPE ); i++ )
			{
				DimensionViewHandle dv = crosstabItem.getDimension( ICrosstabConstants.COLUMN_AXIS_TYPE,
						i );

				if ( dv.getCubeDimension( ) == null )
				{
					throw new CrosstabException( dv.getModelHandle( )
							.getElement( ),
							Messages.getString( "CrosstabQueryHelper.error.invalid.dimension.column", //$NON-NLS-1$
									dv.getCubeDimensionName( ) ) );
				}

				for ( int j = 0; j < dv.getLevelCount( ); j++ )
				{
					LevelViewHandle lv = dv.getLevel( j );

					if ( lv.getCubeLevel( ) == null )
					{
						throw new CrosstabException( lv.getModelHandle( )
								.getElement( ),
								Messages.getString( "CrosstabQueryHelper.error.invalid.level.column", //$NON-NLS-1$
										lv.getCubeLevelName( ) ) );
					}

					columnLevelNameList.add( lv.getCubeLevel( ).getFullName( ) );
				}
			}

		}

		List<IBinding> bindingList = new ArrayList<IBinding>( );

		// add column binding
		Iterator bindingItr = ( (ExtendedItemHandle) crosstabItem.getModelHandle( ) ).columnBindingsIterator( );
		ModuleHandle module = ( (ExtendedItemHandle) crosstabItem.getModelHandle( ) ).getModuleHandle( );

		if ( bindingItr != null )
		{
			Map<String, String> cache = new HashMap<String, String>( );

			DataRequestSession session = DataRequestSession.newSession( new DataSessionContext( DataSessionContext.MODE_DIRECT_PRESENTATION ) );

			try
			{
				IModelAdapter modelAdapter = session.getModelAdaptor( );

				while ( bindingItr.hasNext( ) )
				{
					ComputedColumnHandle column = (ComputedColumnHandle) bindingItr.next( );

					// now user dte model adpater to transform the binding
					IBinding binding = modelAdapter.adaptBinding( column,
							ExpressionLocation.CUBE );

					// still need add aggregateOn field
					List aggrList = column.getAggregateOnList( );

					if ( aggrList != null )
					{
						for ( Iterator aggrItr = aggrList.iterator( ); aggrItr.hasNext( ); )
						{
							String baseLevel = (String) aggrItr.next( );

							addHierachyAggregateOn( module,
									binding,
									column.getExpression( ),
									baseLevel,
									rowLevelNameList,
									columnLevelNameList,
									cache );
						}
					}

					bindingList.add( binding );
				}
			}
			finally
			{
				session.shutdown( );
			}
		}

		return bindingList;
	}

	private static boolean isDataSetFieldReferred( String expression )
	{
		if( expression != null 
				&& ( expression.contains( ExpressionUtil.DATASET_ROW_INDICATOR )
						|| expression.contains( "[DATASET]" ) ) )
		{
			return true;
		}
		
		return false;
	}
	
	public static void addHierachyAggregateOn( ModuleHandle module,
			IBinding binding, String expression, String baseLevel, List<String> rowLevelList,
			List<String> columnLevelList, Map<String, String> cache )
			throws BirtException
	{
		if ( binding == null || baseLevel == null || module == null )
		{
			return;
		}

		boolean isDataSetField = isDataSetFieldReferred( expression );
		int sindex = rowLevelList.indexOf( baseLevel );

		if ( sindex != -1 )
		{
			for ( int i = 0; i <= sindex; i++ )
			{
				String levelName = rowLevelList.get( i );
				String cachedExpression = cache.get( levelName );

				if ( cachedExpression == null )
				{
					cachedExpression = createAggregateLevelExpression( levelName, isDataSetField );
					cache.put( levelName, cachedExpression );
				}

				if ( cachedExpression != null )
				{
					binding.addAggregateOn( cachedExpression );
				}
			}

			// already found on row list, skip on column list
			return;
		}

		sindex = columnLevelList.indexOf( baseLevel );

		if ( sindex != -1 )
		{
			for ( int i = 0; i <= sindex; i++ )
			{
				String levelName = columnLevelList.get( i );
				String cachedExpression = cache.get( levelName );

				if ( cachedExpression == null )
				{
					cachedExpression = createAggregateLevelExpression( levelName, isDataSetField );
					cache.put( levelName, cachedExpression );
				}

				if ( cachedExpression != null )
				{
					binding.addAggregateOn( cachedExpression );
				}
			}

			// already found on column list, skip next
			return;
		}

		// This is possibly an invalid level name to reach here, but we still
		// create the expression for validation.
		String cachedExpression = cache.get( baseLevel );

		if ( cachedExpression == null )
		{
			cachedExpression = createAggregateLevelExpression( baseLevel, isDataSetField );
			cache.put( baseLevel, cachedExpression );
		}

		if ( cachedExpression != null )
		{
			binding.addAggregateOn( cachedExpression );
		}
	}

	private static String createAggregateLevelExpression( String levelFullName, boolean isDataSetField )
	{
		String[] names = CubeUtil.splitLevelName( levelFullName );

		return (isDataSetField) ? ExpressionUtil.createJSDataSetRowExpression( names[1] ) 
				: ExpressionUtil.createJSDimensionExpression( names[0], names[1] );
	}

	private static List<String> getLevelExpressionList(
			CrosstabReportItemHandle crosstab, int axis )
			throws CrosstabException
	{
		List<String> expList = new ArrayList<String>( );

		int count = crosstab.getDimensionCount( axis );

		for ( int i = 0; i < count; i++ )
		{
			DimensionViewHandle dv = crosstab.getDimension( axis, i );
			if ( dv.getCubeDimension( ) == null )
			{
				throw new CrosstabException( dv.getModelHandle( ).getElement( ),
						Messages.getString( "CrosstabQueryHelper.error.invalid.dimension.row", //$NON-NLS-1$
								dv.getCubeDimensionName( ) ) );
			}

			for ( int j = 0; j < dv.getLevelCount( ); j++ )
			{
				LevelViewHandle lv = dv.getLevel( j );

				if ( lv.getCubeLevel( ) == null )
				{
					throw new CrosstabException( lv.getModelHandle( )
							.getElement( ),
							Messages.getString( "CrosstabQueryHelper.error.invalid.level.row", //$NON-NLS-1$
									lv.getCubeLevelName( ) ) );
				}

				String expression = ExpressionUtil.createJSDimensionExpression( dv.getCubeDimension( )
						.getName( ),
						lv.getCubeLevel( ).getName( ) );
				expList.add( expression );
			}
		}

		return expList;
	}

	public static DimensionViewHandle getDimensionViewHandle(
			CrosstabReportItemHandle crosstab, String dimensionShortName )
	{
		DimensionViewHandle dimension = null;

		int type[] = new int[]{
				ROW_AXIS_TYPE, COLUMN_AXIS_TYPE
		};
		for ( int i = 0; i < type.length; i++ )
		{
			int count = crosstab.getDimensionCount( type[i] );
			for ( int j = 0; j < count; j++ )
			{
				DimensionViewHandle tmpDimension = crosstab.getDimension( type[i],
						j );
				if ( tmpDimension != null
						&& tmpDimension.getCubeDimension( )
								.getName( )
								.equals( dimensionShortName ) )
				{
					dimension = tmpDimension;
					return dimension;
				}
			}
		}

		return dimension;

	}

	/**
	 * Check if can merge crosstab header cell.
	 * 
	 * @param crosstab
	 * @return
	 */
	public static boolean canMergeCrosstabHeaderCell(
			CrosstabReportItemHandle crosstab )
	{
		// int[] numbers = CrosstabModelUtil.getHeaderRowAndColumnNumber(
		// crosstab );
		if ( crosstab.getHeaderCount( ) > 1 )
		{
			return true;
		}

		return false;
	}

	/**
	 * Check if can split the crosstab header cell
	 * 
	 * @param crosstab
	 * @return
	 */
	public static boolean canSplitCrosstabHeaderCell(
			CrosstabReportItemHandle crosstab )
	{
		int[] numbers = CrosstabModelUtil.getHeaderRowAndColumnCount( crosstab );
		if ( crosstab.getHeaderCount( ) == 1 && numbers[0] * numbers[1] > 1 )
		{
			return true;
		}

		return false;
	}

	/**
	 * Merge all crosstab header cell
	 * 
	 * @param crosstab
	 */
	public static void mergeCrosstabHeaderCell(
			CrosstabReportItemHandle crosstab )
	{
		int count = crosstab.getHeaderCount( );
		if ( count <= 1 )
		{
			return;
		}
		PropertyHandle headerHandle = crosstab.getModelHandle( )
				.getPropertyHandle( ICrosstabReportItemConstants.HEADER_PROP );
		for ( int i = 1; i < count; i++ )
		{
			try
			{
				headerHandle.removeItem( 1 );
			}
			catch ( PropertyValueException e )
			{
				// do nothing now
			}
		}
	}

	/**
	 * Splite the crosstab header cell
	 * 
	 * @param crosstab
	 */
	public static void splitCrosstabHeaderCell(
			CrosstabReportItemHandle crosstab )
	{
		int[] numbers = CrosstabModelUtil.getHeaderRowAndColumnCount( crosstab );
		int total = numbers[0] * numbers[1];
		PropertyHandle headerHandle = crosstab.getModelHandle( )
				.getPropertyHandle( ICrosstabReportItemConstants.HEADER_PROP );
		for ( int i = 1; i < total; i++ )
		{
			ExtendedItemHandle cellHandle = CrosstabExtendedItemFactory.createCrosstabCell( crosstab.getModuleHandle( ) );

			try
			{
				headerHandle.add( cellHandle );
			}
			catch ( SemanticException e )
			{
				// do nothing now
			}
		}

	}

	/**
	 * @return Returns the row and column count for crosstab header cells
	 */
	public static int[] getCrosstabHeaderRowAndColumnCount(
			CrosstabReportItemHandle crosstab )
	{
		return CrosstabModelUtil.getHeaderRowAndColumnCount( crosstab );
	}

	public static int findPriorLevelCount( DimensionViewHandle viewHandle )
	{
		return CrosstabModelUtil.findPriorLevelCount( viewHandle );
	}

	public static List<LevelViewHandle> getLevelList(
			CrosstabReportItemHandle crosstab, int axisType )
	{
		return CrosstabModelUtil.getLevelList( crosstab, axisType );
	}
	
	/**Add all the label to the header cell
	 * @param reportHandle
	 */
	public static void addAllHeaderLabel(CrosstabReportItemHandle reportHandle)
	{
		for (int i=0; i<reportHandle.getDimensionCount( ICrosstabConstants.ROW_AXIS_TYPE ); i++)
		{
			DimensionViewHandle viewHandle = reportHandle.getDimension( ICrosstabConstants.ROW_AXIS_TYPE, i );
			for (int j=0; j<viewHandle.getLevelCount( ); j++)
			{
				addLabelToHeader( viewHandle.getLevel( j ) );
			}
		}
		for (int i=0; i<reportHandle.getDimensionCount( ICrosstabConstants.COLUMN_AXIS_TYPE ); i++)
		{
			DimensionViewHandle viewHandle = reportHandle.getDimension( ICrosstabConstants.COLUMN_AXIS_TYPE, i );
			for (int j=0; j<viewHandle.getLevelCount( ); j++)
			{
				addLabelToHeader( viewHandle.getLevel( j ) );
			}
		}
	}
	
	private static boolean isShowColumnMeasureHeader(CrosstabReportItemHandle crosstab)
	{
		return (!crosstab.isHideMeasureHeader( )) && crosstab.getMeasureCount( ) != 0 && ICrosstabConstants.MEASURE_DIRECTION_HORIZONTAL.equals( crosstab.getMeasureDirection( ));
	}
	/**Add the label to the header cell. 
	 * @param levelHandle
	 */
	public static void addLabelToHeader(LevelViewHandle levelHandle)
	{
//		if (ICrosstabConstants.COLUMN_AXIS_TYPE == levelHandle.getAxisType( ))
//		{
//			//addAllHeaderLabel( levelHandle.getCrosstab( ) );
//			return;
//		}
		
		int type = levelHandle.getAxisType( );
		
		
		DimensionViewHandle viewHandle = (DimensionViewHandle)levelHandle.getContainer( );
		int count = CrosstabUtil.findPriorLevelCount( viewHandle ) + levelHandle.getIndex( );
		
		CrosstabReportItemHandle crosstab = levelHandle.getCrosstab( );
		boolean addForce = false;
		int[] numbers = CrosstabUtil.getCrosstabHeaderRowAndColumnCount( crosstab );
		if (ICrosstabConstants.COLUMN_AXIS_TYPE == type && !isShowColumnMeasureHeader( crosstab ) && count == numbers[0] - 1)
		{
			if (findPreLeveHandle( levelHandle ) != null)
			{
				addLabelToHeader( findPreLeveHandle( levelHandle ) );
			}
			if (getLevelList( crosstab,
					ICrosstabConstants.ROW_AXIS_TYPE ).size( ) != 0)
			{
				if (!( ICrosstabConstants.MEASURE_DIRECTION_VERTICAL.equals( crosstab.getMeasureDirection( )) && crosstab.getMeasureCount( ) != 0) || crosstab.isHideMeasureHeader( ))
				{
					return;
				}
				else
				{
					addForce = true;
				}
			}
		}
		else if (ICrosstabConstants.ROW_AXIS_TYPE == type && numbers[0]*numbers[1] == 1)
		{
			addForce = true;
		}
		if (ICrosstabConstants.COLUMN_AXIS_TYPE == type)
		{
			count = (count + 1)*(numbers[1]) - 1;
		}
		else
		{
			count = (numbers[0]-1)*(numbers[1])+count;	
		}
		try
		{
			if (crosstab.getHeader( count ) != null && crosstab.getHeader( count ).getContents( ).size( ) == 0)
			{
				LabelHandle labelHandle = crosstab.getModuleHandle( )
						.getElementFactory( )
						.newLabel( null );
				
				labelHandle.setText(levelHandle.getDisplayField( ) == null? levelHandle.getCubeLevel( ).getName( ): levelHandle.getDisplayField( ));
				
				crosstab.getHeader( count ).addContent( labelHandle );
			}
			else if (crosstab.getHeader( count ) != null && addForce)
			{
				if ( crosstab.getHeader( count ).getContents( ).get( 0 ) instanceof LabelHandle)
				{
					LabelHandle labelHandle = (LabelHandle)crosstab.getHeader( count ).getContents( ).get( 0 );
					labelHandle.setText(levelHandle.getDisplayField( ) == null? levelHandle.getCubeLevel( ).getName( ): levelHandle.getDisplayField( ));
				}
			}
		}
		catch ( SemanticException e )
		{
			//Do nothing now
		}
	}
	
	private static LevelViewHandle findPreLeveHandle(LevelViewHandle handle)
	{
		DimensionViewHandle viewHandle = (DimensionViewHandle)handle.getContainer( );
		if (handle.getIndex( ) != 0)
		{
			return viewHandle.getLevel( handle.getIndex( ) - 1 );
		}
		if (viewHandle.getIndex( ) == 0)
		{
			return null;
		}
		
		viewHandle = handle.getCrosstab( ).getDimension( viewHandle.getAxisType( ), viewHandle.getIndex( ) - 1);
		return viewHandle.getLevel( viewHandle.getLevelCount( ) - 1 );
	}
}
