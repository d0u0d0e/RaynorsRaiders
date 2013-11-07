package javabot.RaynorsRaiders;

import java.awt.Point;
import java.util.LinkedList;

import javabot.JNIBWAPI;
import javabot.model.BaseLocation;
import javabot.model.Unit;
import javabot.types.TechType;
import javabot.types.TechType.TechTypes;
import javabot.types.UnitType;
import javabot.types.UnitType.UnitTypes;
import javabot.types.UpgradeType;
import javabot.types.UpgradeType.UpgradeTypes;
import javabot.util.BWColor;
import javabot.RaynorsRaiders.CoreReactive.*; // Why do we need this line? -Matt

// Cannot import core reactive, primary and secondary constructors will init the AI core communication

public class ManagerBuild extends RRAITemplate{
	
	private JNIBWAPI bwapi;
	CoreReactive core;
	CoreReactive.BuildMode mode;
	LinkedList<UnitTypes> orders;
	LinkedList<UnitTypes> builtBuildings;
	LinkedList<UnitTypes> buildingBuildings;
	//LinkedList<UnitTypes> Buildings;
	LinkedList<BaseLocation> ourBases;
	
	UnitTypes tempType;
	Unit tempUnit;
	
	int homePositionX;
	int homePositionY;
	
	public ManagerBuild() {
		//SET UP ALL INTERNAL VARIABLES HERE
		super();
		builtBuildings = new LinkedList<UnitTypes>();
		builtBuildings.push(UnitTypes.Terran_Command_Center);
		ourBases = new LinkedList<BaseLocation>();
	}
	
	public enum baseStatus 
	{
		UNOCCUPIED, BUILT, BUILDING, 
		DESTROYED, ENEMIES, ENEMIES_DESTROYED
	}
	
	private class BaseRR
	{
		private BaseLocation baseLoc;
		private int baseNumber;
		private baseStatus status;
		
		private BaseRR() 
		{	
		}
		
		private BaseRR(BaseLocation bl, int number, baseStatus status)
		{
			this.baseLoc = bl;
			this.baseNumber = number;
			this.status = status;
		}
	
	}
	
	/*
	 * Assumes that this function will be called on the first frame before
	 * we have other bases
	 */
	
	public int baseSetup()
	{
		int unitPostX = 0, unitPostY = 0;
		int cc = getNearestUnit(UnitTypes.Terran_Command_Center.ordinal(), 0, 0);
		unitPostX = bwapi.getUnit(cc).getX();
		unitPostY = bwapi.getUnit(cc).getY();
		for (BaseLocation b : bwapi.getMap().getBaseLocations())
		{
			if (b.getX() == unitPostX && b.getY() == unitPostY)
			{
				ourBases.add(b);
				System.out.println("ManagerBuild: Found our home base! Size is " + ourBases.size());
			}
		}
		return ourBases.size();
	}
	
	/*
	 * Returns the location of our very first base
	 */
	
	public BaseLocation getStartLocation() 
	{
		return ourBases.get(0);
	}
	
	public void getOurBases() 
	{
		
	}
	
	/*
	 * 	if ((unit.getTypeID() == buildingTypeID) && (!unit.isCompleted())) return true;
	 * 
	 * 
	 */
	
	public void AILinkManagerBuild(JNIBWAPI d_bwapi, CoreReactive d_core) {
		//Here you get your pointers to the other AI cores (JINBWAPI, core, ect ect ect)
		//The Raynors Raiders code should call this "constructor" after all the other AI parts have
		// been created.
		bwapi = d_bwapi;
		core = d_core;
		mode = core.econ_getBuildingMode();
		orders = core.econ_getBuildingStack();
	}
	
	public void captureBaseLocation() {
		// Remember our homeTilePosition at the first frame
//		if (bwapi.getFrameCount() == 1) {
			int cc = getNearestUnit(UnitTypes.Terran_Command_Center.ordinal(), 0, 0);
			homePositionX = bwapi.getUnit(cc).getX();
			homePositionY = bwapi.getUnit(cc).getY();
//		}
	}
	
	
	public int underConstructionM() {
		int cost = 0;
		
		for(Unit unit : bwapi.getMyUnits()) {
			if(unit.isConstructing()) {
				UnitType bldg = bwapi.getUnitType(unit.getBuildTypeID());
				
				cost += bldg.getMineralPrice();
			}
		}
		
		return cost;
	}
	
	public int underConstructionG() {
		int cost = 0;
		
		for(Unit unit : bwapi.getMyUnits()) {
			if(unit.isConstructing()) {
				UnitType bldg = bwapi.getUnitType(unit.getBuildTypeID());
				
				cost += bldg.getGasPrice();
			}
		}
		
		return cost;
	}
	
	// looks for a building to construct according to the build mode
	// calls build() method if it finds something to construct
	public void construct() {
//System.out.println("orders: " + orders.toString());
//System.out.println("built: " +builtBuildings.toString());
		switch(mode) {
			case FIRST_POSSIBLE:
				int i = 0;
				boolean canBuild = false;
				UnitTypes b = null;
				UnitType bldg = null;
				
				// go through list until we find a buildable structure
				while(!canBuild && i < orders.size()){
					b = orders.get(i);
					bldg = bwapi.getUnitType(b.ordinal());
					
					if(bwapi.getSelf().getMinerals() < bldg.getMineralPrice() && bwapi.getSelf().getGas() < bldg.getGasPrice()) {
						i++;
					}
					else {
						canBuild = true;
					}
				}
				
				if(canBuild){
				   build(b);
				   orders.remove(i);
				   builtBuildings.add(b);
				}
				else {
					System.out.println("could not build anything in stack");
				}
					
				break;
			case BLOCKING_STACK:
				b = orders.peek();
				bldg = bwapi.getUnitType(b.ordinal());
				
				if(bwapi.getSelf().getMinerals() - underConstructionM() >= bldg.getMineralPrice() && bwapi.getSelf().getGas() - underConstructionG() >= bldg.getGasPrice()) {
					build(b);
					orders.pop();
					builtBuildings.add(b);
				}

				break;
			case HOLD_ALL:
				System.out.println("halting construction...");
				break;
			default:
				break;
		}
	}
	
	
        // input: build(UnitTypes.xxxx) 
        // will build near similar building types
        // will also build add-ons (not sure if structure will attempt relocating if there's not enough room)
	public void build(UnitTypes structure) {
		UnitType bldg;
		
		if (structure == null)
		{
			return;
		}
		if (structure.ordinal() == UnitTypes.Terran_Command_Center.ordinal())
		{
			buildExpand();
		}
		
		bldg = bwapi.getUnitType(structure.ordinal());
		
		if (bwapi.getSelf().getMinerals() >= bldg.getMineralPrice()) {
		  if(bwapi.getSelf().getGas() >= bldg.getGasPrice()) {
			if(bldg.isAddon()) {
				//find parent structure
				for(Unit unit : bwapi.getMyUnits()) {
					if(unit.getTypeID() == bldg.getWhatBuildID()) {
						bwapi.buildAddon(unit.getID(), bldg.getID());
						break;
					}
				}
				
			} else {
			// try to find the worker near our home position
				int worker = getNearestUnit(UnitTypes.Terran_SCV.ordinal(), homePositionX, homePositionY);
				if (worker != -1) {
				// if we found him, try to select appropriate build tile position for bldg (near our home base)
					int xtile = homePositionX, ytile = homePositionY;
				
					for(Unit unit : bwapi.getMyUnits()) {
						if(unit.getTypeID() == bldg.getID()) {
							xtile = unit.getX();
							ytile = unit.getY();
							break;
						}
					}
				    Point buildTile = getBuildTile(worker, bldg.getID(), xtile, ytile);
				    // if we found a good build position, and we aren't already constructing a bldg 
				    // order our worker to build it
				    if ((buildTile.x != -1) && (!weAreBuilding(bldg.getID()))) {
				    	bwapi.build(worker, buildTile.x, buildTile.y, bldg.getID());
				    	//buildingBuildings.push(bldg);
				    }
				}
				else {
					core.core_econ_buildAlerts.push(BuildAlert.NO_WORKERS);
				}
			}
		  }
		  else {
			  core.core_econ_buildAlerts.push(BuildAlert.NO_GAS);
		  }
		}
		else {
			core.core_econ_buildAlerts.push(BuildAlert.NO_MINERALS);
		}
	}
	
	public void buildExpand()
	{
		
	}

    // input: upgrade(UpgradeTypes.xxxx)
    // method will do resource check before attempting upgrade
    // so far, will only upgrade at base price
	public void upgrade(UpgradeTypes lvlup) {
		UpgradeType gear = bwapi.getUpgradeType(lvlup.ordinal());
	
		if(bwapi.getSelf().getMinerals() >= gear.getMineralPriceBase()) {
		  if(bwapi.getSelf().getGas() >= gear.getGasPriceBase()) {
			for(Unit unit : bwapi.getMyUnits()) {
				if(unit.getTypeID() == gear.getWhatUpgradesTypeID()) {
					bwapi.upgrade(unit.getID(), gear.getID());
				}
			}
		}
		else {
			core.core_econ_buildAlerts.push(BuildAlert.NO_GAS);
		}
	 }
	 else {
		 core.core_econ_buildAlerts.push(BuildAlert.NO_MINERALS);
	 }
	}

   // input: research(TechTypes.xxxx)
    // method will do resource check before attempting research
	public void research(TechTypes item) {
		TechType tech = bwapi.getTechType(item.ordinal());
	
		if(bwapi.getSelf().getMinerals() >= tech.getMineralPrice()) {
		  if(bwapi.getSelf().getGas() >= tech.getGasPrice()) {
		
			for(Unit unit : bwapi.getMyUnits()) {
				if(unit.getTypeID() == tech.getWhatResearchesTypeID()) {
					bwapi.research(unit.getID(), tech.getID());
				}
			}
		  }
		  else {
				core.core_econ_buildAlerts.push(BuildAlert.NO_GAS);
		  }
	    }
		else {
			 core.core_econ_buildAlerts.push(BuildAlert.NO_MINERALS);
		}
	}


	//Returns the id of a unit of a given type, that is closest to a pixel position (x,y), or -1 if we
	//don't have a unit of this type
	public int getNearestUnit(int unitTypeID, int x, int y) {
		int nearestID = -1;
		double nearestDist = 9999999;
		for (Unit unit : bwapi.getMyUnits()) {
			if ((unit.getTypeID() != unitTypeID) || (!unit.isCompleted())) continue;
			double dist = Math.sqrt(Math.pow(unit.getX() - x, 2) + Math.pow(unit.getY() - y, 2));
			if (nearestID == -1 || dist < nearestDist) {
				nearestID = unit.getID();
				nearestDist = dist;
 			}
 		}
 		return nearestID;
	}	
	
	// This is so AIs can link data if they need to
	// they only need to rewrite this function in
	// their code
	public void AILinkData() {
		//Remember by this time all AI pointers are pointing to their respective AIs
		//So you can use react.whatever, baby.something, ect
		AILinkManagerBuild(super.bwapi, super.react);
	}
	
	

	//Returns the Point object representing the suitable build tile position
	//for a given building type near specified pixel position (or Point(-1,-1) if not found)
	//(builderID should be our worker)
	public Point getBuildTile(int builderID, int buildingTypeID, int x, int y) {
		Point ret = new Point(-1, -1);
		int maxDist = 3;
		int stopDist = 40;
		int tileX = x/32; int tileY = y/32;
	
		// Refinery, Assimilator, Extractor
		if (bwapi.getUnitType(buildingTypeID).isRefinery()) {
			for (Unit n : bwapi.getNeutralUnits()) {
				if ((n.getTypeID() == UnitTypes.Resource_Vespene_Geyser.ordinal()) && 
						( Math.abs(n.getTileX()-tileX) < stopDist ) &&
						( Math.abs(n.getTileY()-tileY) < stopDist )
						) return new Point(n.getTileX(),n.getTileY());
			}
		}
	
		while ((maxDist < stopDist) && (ret.x == -1)) {
			for (int i=tileX-maxDist; i<=tileX+maxDist; i++) {
				for (int j=tileY-maxDist; j<=tileY+maxDist; j++) {
					if (bwapi.canBuildHere(builderID, i, j, buildingTypeID, false)) {
						// units that are blocking the tile
						boolean unitsInWay = false;
						for (Unit u : bwapi.getAllUnits()) {
							if (u.getID() == builderID) continue;
							if ((Math.abs(u.getTileX()-i) < 4) && (Math.abs(u.getTileY()-j) < 4)) unitsInWay = true;
						}
						if (!unitsInWay) {
							ret.x = i; ret.y = j;
							return ret;
						}
						// creep for Zerg (this may not be needed - not tested yet)
						if (bwapi.getUnitType(buildingTypeID).isRequiresCreep()) {
							boolean creepMissing = false;
							for (int k=i; k<=i+bwapi.getUnitType(buildingTypeID).getTileWidth(); k++) {
								for (int l=j; l<=j+bwapi.getUnitType(buildingTypeID).getTileHeight(); l++) {
									if (!bwapi.hasCreep(k, l)) creepMissing = true;
									break;
								}
							}
							if (creepMissing) continue; 
						}
						// psi power for Protoss (this seems to work out of the box)
						if (bwapi.getUnitType(buildingTypeID).isRequiresPsi()) {}
					}
				}
			}
			maxDist += 2;
		}
	
		if (ret.x == -1) bwapi.printText("Unable to find suitable build position for "+bwapi.getUnitType(buildingTypeID).getName());
		return ret;
	}

	//Returns true if we are currently constructing the building of a given type.
	public boolean weAreBuilding(int buildingTypeID) {
		for (Unit unit : bwapi.getMyUnits()) {
			if ((unit.getTypeID() == buildingTypeID) && (!unit.isCompleted())) return true;
			if (bwapi.getUnitType(unit.getTypeID()).isWorker() && unit.getConstructingTypeID() == buildingTypeID) return true;
		}
		return false;
	}

}
