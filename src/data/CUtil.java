package data;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import massim.scenario.city.data.Item;
import massim.scenario.city.data.Job;

public class CUtil {

    
    public static Map<String, Integer> extractItems(Job job) {
		return job.getRequiredItems().toItemAmountData().stream()
				.collect(Collectors.toMap(x -> x.getName(), x -> x.getAmount()));
    }

	public static Map<String, Integer> toStringMap(Map<Item, Integer> items) {
		return items.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey().getName(), Entry::getValue));
	}

	public static String getName(Entry<Item, Integer> entry) {
		return entry.getKey().getName();
	}
	
	/**
	 * @param map The map to add the content to
	 * @param first The element in the first map, which the content should be added to 
	 * @param second The element in the second map, to which the content should be addad
	 * @param content The content to add
	 */
	public static <A,B,C> void addToMapOfMaps(Map<A, Map<B, C>> map, A first, B second, C content) 
	{
		if (!map.containsKey(first))
		{
			map.put(first, new HashMap<B, C>());
		}
		map.get(first).put(second, content);
	}

}
