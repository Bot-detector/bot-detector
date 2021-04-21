package com.botdetector.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class PredictionFeedback
{
	@SerializedName("rsn")
	String voterName;

	@SerializedName("voter_id")
	int voterID;

	@SerializedName("subject_id")
	int subjectID;

	//1 for good, -1 for bad
	int vote;

	@SerializedName("prediction")
	String predictionLabel;

	@SerializedName("confidence")
	double predictionConfidence;

}
