package group30;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import genius.core.AgentID;
import genius.core.Bid;
import genius.core.Domain;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.OutcomeSpace;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.misc.Range;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;
import genius.core.bidding.BidDetailsSorterUtility;

import java.util.HashMap;
import java.util.stream.Collectors;


public class Agent30 <bid> extends AbstractNegotiationParty {
    public static void main(String[] args) {
        //System.out.println("Hello Agent!");
    }
    private final String description = "Agent30";

    private Bid lastReceivedOffer; // offer on the table
    private Bid myLastOffer;
    private HashMap<String, HashMap> issuesOpp = new HashMap<>();
    private AdditiveUtilitySpace opponentsAdditiveUtilitySpace;
    private int counterOfOffers = 0;
    private double delta;
    private int counter=1;
    private double upper=1.1;
    private int count=0;
    private int i = 0;
    private List<BidDetails> bestbids = new ArrayList<BidDetails>();

    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        if (hasPreferenceUncertainty()) {
            System.out.println("Preference uncertainty is enabled.");

            genius.core.uncertainty.BidRanking bidRanking = userModel.getBidRanking();
            System.out.println("The agent ID is:" + info.getAgentID());
            System.out.println("Total number of possible possibleBids:" + userModel.getDomain().getNumberOfPossibleBids());
            System.out.println("The number of possibleBids in the ranking is:" + bidRanking.getSize());
            System.out.println("The lowest bid is:" + bidRanking.getMinimalBid());
            System.out.println("The highest bid is:" + bidRanking.getMaximalBid());
            System.out.println("The elicitation costs are:" + user.getElicitationCost());
            List<bid> bidList = (List<bid>) bidRanking.getBidOrder();
            System.out.println("The 5th bid in the ranking is:" + bidList.get(4));
        }
        Domain domain = getDomain();
        System.out.println(domain);
        delta = 0.000001 * domain.getNumberOfPossibleBids();
        System.out.println("delta"+delta);
        AbstractUtilitySpace utilitySpace = info.getUtilitySpace();
        AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;
        opponentsAdditiveUtilitySpace = (AdditiveUtilitySpace) additiveUtilitySpace.copy();
        List<Issue> issues = additiveUtilitySpace.getDomain().getIssues();
        for (Issue issue : issues) {
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            HashMap<String,Integer> valuesMapOpp = new HashMap<String, Integer>();

            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
                valuesMapOpp.put(valueDiscrete.getValue(),0);
            }
            issuesOpp.put(issue.getName(),valuesMapOpp);
        }
    }


    public Bid randBidsWithThreshold(double Threshold)
    {
        double utility;
        Bid randBid;

        do
        {
            randBid = generateRandomBid();
            try {
                utility = utilitySpace.getUtility(randBid);
            } catch (Exception e)
            {
                utility = 0.0;
            }
        }
        while (utility < Threshold);
        return randBid;
    }

    public Bid CreateBidInRange(double Threshold, double upperlim) {
        double time = getTimeLine().getTime();
        OutcomeSpace outcomeSpace = new OutcomeSpace(utilitySpace);
        //List<BidDetails> possibleBids = outcomeSpace.getBidsinRange(new Range(0.8, Threshold));
        List<BidDetails> possibleBids = outcomeSpace.getBidsinRange(new Range(0.7,1.1));
        System.out.println("Length of Bids " + possibleBids.size());
        if(possibleBids.size() < 1) {
            try {
                return utilitySpace.getMaxUtilityBid();
            } catch (Exception e) {
                return randBidsWithThreshold(Threshold);
            }
        }
        else if(possibleBids.size()> 1000){
            System.out.println("Reducing Bid Size");
            possibleBids = possibleBids.stream().limit(1000).collect(Collectors.toList());
        }
        Collections.sort(possibleBids, new BidDetailsSorterUtility());
        System.out.println(possibleBids);
        double nextBidUtility = 0;

        BidDetails nextBid = possibleBids.get(0);
        System.out.println("Length of Bids " + possibleBids.size());
        //Compute nash products and offer the bid with higher nash product
        int jump = rand.nextInt(2);

        for (i = count; i < possibleBids.size() - 1; i++) {
            //for (BidDetails bidDetails : possibleBids) {
            //double bidUtility = bidDetails.getMyUndiscountedUtil() * opponentsAdditiveUtilitySpace.getUtility(bidDetails.getBid());
            double bidUtility = possibleBids.get(i).getMyUndiscountedUtil() * opponentsAdditiveUtilitySpace.getUtility(possibleBids.get(i).getBid());

            //if((nextBidUtility < bidUtility) && (opponentsAdditiveUtilitySpace.getUtility(bidDetails.getBid()) > bidDetails.getMyUndiscountedUtil()))
            if ((nextBidUtility < bidUtility)) //&& (opponentsAdditiveUtilitySpace.getUtility(possibleBids.get(i).getBid()) > possibleBids.get(i).getMyUndiscountedUtil()))
            {
                nextBidUtility = bidUtility;
                nextBid = possibleBids.get(i);
                this.bestbids.add(new BidDetails(nextBid.getBid(), nextBidUtility));
                Collections.sort(bestbids, new BidDetailsSorterUtility());
            }
        }

        //if(counter %2 == 0) {
        count += 1;
        //}


        System.out.println("BidList" + possibleBids);
        //System.out.println("10th bid" + possibleBids.get(i));
        System.out.println("nextBid " + nextBid);
        System.out.println("nextBidUtility " + nextBidUtility);
        System.out.println(opponentsAdditiveUtilitySpace.getUtility(nextBid.getBid()));
        upper = nextBid.getMyUndiscountedUtil();
        System.out.println(nextBid.getMyUndiscountedUtil());
        System.out.println(opponentsAdditiveUtilitySpace.getUtility(nextBid.getBid()) * nextBid.getMyUndiscountedUtil());
        if (time > 0.6 && time <= 0.9){
            System.out.println("JumpVal" + jump);
            if (jump == 0){
                return nextBid.getBid();
            }
            else {
                Bid bestbid;
                int randomindex = rand.nextInt(10);
                bestbid = this.bestbids.get(randomindex).getBid();
                System.out.println("Bestbidutility"+ utilitySpace.getUtility(bestbid));
                return bestbid;
            }
        }
        else if(time > 0.9){
            double bestopputility = 0;
            Bid bestbid = nextBid.getBid();
            for(BidDetails bidDetails: bestbids)
            {

                double opputility = opponentsAdditiveUtilitySpace.getUtility(bidDetails.getBid());
                if(opputility > bestopputility){
                    bestopputility = opputility;
                    bestbid = bidDetails.getBid();
                }
            }
            return bestbid;
        }

        return nextBid.getBid();

    }


    public int findC (double time) {
        int C=0;
        if ((time>0.1) && (time<=1)) {
            C=13;

        }
        else if (time<=0.1) {
            C = 8;

        }
        return C;
    }


    @Override
    public Action chooseAction(List<Class<? extends Action>> list) {
        double time = getTimeLine().getTime();
        int C = findC(time);
        double utility = (double) (Math.log(1- time) / C) + 1d;
        System.out.println("utility" + utility);
        if (lastReceivedOffer != null && myLastOffer != null) {
            System.out.println("counter " + counter);
            counter += 1;
            System.out.println("our utility in my offer" + this.utilitySpace.getUtility(myLastOffer));
            System.out.println("our utility in opponent offer" + this.utilitySpace.getUtility(lastReceivedOffer));
            System.out.println("nash last received " + this.utilitySpace.getUtility(lastReceivedOffer) * opponentsAdditiveUtilitySpace.getUtility(lastReceivedOffer));
            System.out.println("nash my last offer "+ this.utilitySpace.getUtility(myLastOffer) * opponentsAdditiveUtilitySpace.getUtility(myLastOffer));
            System.out.println("our utility last received " + this.utilitySpace.getUtility(lastReceivedOffer));
            System.out.println("opp utilitylast received "+ opponentsAdditiveUtilitySpace.getUtility(lastReceivedOffer));
            System.out.println("our utility my last " + this.utilitySpace.getUtility(myLastOffer));
            System.out.println("opp utilitylast my last " + opponentsAdditiveUtilitySpace.getUtility(myLastOffer));
            System.out.println(" ");
        }
        if (time<0.2) {
            myLastOffer=getMaxUtilityBid();
            if (lastReceivedOffer != null
                    && myLastOffer != null
                    && this.utilitySpace.getUtility(lastReceivedOffer)> this.utilitySpace.getUtility(myLastOffer)) {

                return new Accept(this.getPartyId(), lastReceivedOffer);
            }
            else {
                return new Offer(this.getPartyId(), myLastOffer);
            }
        }
        else  {
            System.out.println("0.1<time0.9<: CreateBidInRange");
            myLastOffer = CreateBidInRange(utility, 1.1);
            //myLastOffer=CreateBidInRange(utility);
            System.out.println("my next offer utility" + this.utilitySpace.getUtility(myLastOffer));
            if (lastReceivedOffer != null
                    && myLastOffer != null
                    && this.utilitySpace.getUtility(lastReceivedOffer) > this.utilitySpace.getUtility(myLastOffer)) {

                return new Accept(this.getPartyId(), lastReceivedOffer);
            } else {
                //myLastOffer=CreateBidInRange(utility);
                return new Offer(this.getPartyId(), myLastOffer);
            }
        }


    }







    //else {
    //System.out.println("time>0.8: randBidsWithThreshold");
    //if (this.utilitySpace.getUtility(lastReceivedOffer)>utility) {
    //System.out.println("Accepted");
    //return new Accept(this.getPartyId(), lastReceivedOffer);}
    //else {
    //myLastOffer=CreateBidInRange(utility, 1.1);
    //myLastOffer=randBidsWithThreshold(utility);
    //return new Offer(this.getPartyId(), myLastOffer);
    //}
    //}



    private int JohnyBlackSort(int currFreq, HashMap valuesMapOpp, List<ValueDiscrete> keys) {
        int nextBid = 0;
        for(ValueDiscrete valueDiscrete : keys) {
            String key = valueDiscrete.getValue();
            int freq = (int) valuesMapOpp.get(key);
            if(freq > currFreq) {
                nextBid += 1;
            }
        }
        return nextBid + 1;
    }

    @Override
    public void receiveMessage(AgentID sender, Action act) {
        super.receiveMessage(sender, act);

        if (act instanceof Offer) {
            Offer offer = (Offer) act;
            lastReceivedOffer = offer.getBid();
            computewightsopponent(lastReceivedOffer);
        }
    }


    public void computewightsopponent(Bid lastReceivedOffer) {
        List<Issue> issues = lastReceivedOffer.getIssues();
        for (Issue issue : issues) {
            EvaluatorDiscrete ed = new EvaluatorDiscrete();
            String issueKey = issue.getName();
            HashMap valuesMapOpp = issuesOpp.get(issueKey);
            Value PreferenceOpp = lastReceivedOffer.getValue(issue.getNumber());
            valuesMapOpp.put(PreferenceOpp, new Integer((int)valuesMapOpp.get(PreferenceOpp) + 1));
            valuesMapOpp.get(lastReceivedOffer.getValue(issue.getNumber()));
            HashMap<String, Double> ValuesOpOpp = new HashMap<>();
            if(counterOfOffers >= 10)
            {
                double nextBid = 0;
                int NumberOfFreq = 0;
                IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
                ArrayList<Integer> frequencies = new ArrayList<>();

                int options = issueDiscrete.getValues().size();

                for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
                    String valueKey = valueDiscrete.getValue();
                    int frequency = (int) valuesMapOpp.get(valueKey);
                    int ranking = JohnyBlackSort(frequency, valuesMapOpp, issueDiscrete.getValues());
                    if (options==0) {
                        options = 1;
                    }
                    double temp1 = options-ranking+1;
                    double temp2 = (double) options;
                    double estim = temp1/temp2;
                    ValuesOpOpp.put(valueKey, estim);
                    frequencies.add(frequency);
                    NumberOfFreq = NumberOfFreq+frequency;
                    ed.setEvaluationDouble(valueDiscrete, estim);
                }
                for(int frequency : frequencies) {
                    nextBid += Math.pow((double) frequency, 2) / Math.pow((double) NumberOfFreq,2);
                }
                double estimatedIssueWeight = nextBid;
                ed.setWeight(estimatedIssueWeight);
                opponentsAdditiveUtilitySpace.addEvaluator(issue, ed);
            }
        }
        if(counterOfOffers >= 10) {
            opponentsAdditiveUtilitySpace.normalizeWeights();
            counterOfOffers = 0;}
        else {
            counterOfOffers += 1;}
    }

    @Override
    public String getDescription() {
        return description;
    }

    private Bid getMaxUtilityBid() {
        try {
            return this.utilitySpace.getMaxUtilityBid();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

